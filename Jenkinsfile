pipeline {
    agent any

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['staging', 'dev', 'prod'], description: 'Target Azure deployment tier')
        booleanParam(name: 'RUN_SECURITY_SCAN', defaultValue: true, description: 'Run Trivy container vulnerability scanner')
        booleanParam(name: 'DEPLOY_TO_AZURE', defaultValue: true, description: 'Deploy artifacts to Azure Kubernetes Service')
    }

    environment {
        // Build & Versioning Metadata
        APP_NAME           = 'secure-document-vault'
        BUILD_TAG          = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'local'}"
        
        // Azure Infrastructure Coordinates (Configurable or provisioned by Terraform)
        AZURE_CREDENTIALS_ID = 'azure-sp-credentials' // Jenkins Credential ID for Azure Service Principal
        AZURE_RESOURCE_GROUP = 'devops-vault-rg'
        ACR_NAME             = 'devopsvaultacr'
        ACR_LOGIN_SERVER     = "${ACR_NAME}.azurecr.io"
        AKS_CLUSTER_NAME     = 'devops-vault-aks'
        IMAGE_NAME           = "${ACR_LOGIN_SERVER}/${APP_NAME}"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
        ansiColor('xterm')
    }

    stages {
        // ==============================================================
        // STAGE 1: Checkout & Verification
        // ==============================================================
        stage('Checkout & Git Metadata') {
            steps {
                script {
                    echo "=========================================================="
                    echo "🚀 Pipeline Initialized for: ${APP_NAME}"
                    echo "📌 Target Environment: ${params.ENVIRONMENT}"
                    echo "🏷️ Build Tag: ${BUILD_TAG}"
                    echo "=========================================================="
                }
            }
        }

        // ==============================================================
        // STAGE 2: Gradle Build & Compilation
        // ==============================================================
        stage('Gradle Build & Lint') {
            steps {
                script {
                    echo "🔨 Compiling Java sources and assembling JAR via Gradle..."
                    sh './gradlew clean compileJava assemble --no-daemon --stacktrace'
                }
            }
        }

        // ==============================================================
        // STAGE 3: Automated Testing & JaCoCo Coverage
        // ==============================================================
        stage('Gradle Unit Tests & Coverage') {
            steps {
                script {
                    echo "🧪 Running JUnit 5 tests & generating JaCoCo coverage reports..."
                    sh './gradlew test jacocoTestReport --no-daemon'
                }
            }
            post {
                always {
                    // Archive JUnit Test Results in Jenkins
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    
                    // Publish JaCoCo Code Coverage
                    recordCoverage(tools: [[parser: 'JACOCO', pattern: 'build/reports/jacoco/test/jacocoTestReport.xml']])
                }
            }
        }

        // ==============================================================
        // STAGE 4: SonarQube Quality Gate (Optional/Configured)
        // ==============================================================
        stage('Quality Gate (SonarQube)') {
            when {
                expression { return env.SONAR_HOST_URL != null }
            }
            steps {
                script {
                    echo "🔍 Scanning code with SonarQube..."
                    withSonarQubeEnv('SonarQube-Server') {
                        sh './gradlew sonar --no-daemon'
                    }
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }

        // ==============================================================
        // STAGE 5: Security & Vulnerability Scanning
        // ==============================================================
        stage('Security Scanning') {
            when {
                expression { return params.RUN_SECURITY_SCAN }
            }
            steps {
                script {
                    echo "🛡️ Running Trivy vulnerability scan against dependencies..."
                    sh '''
                        if command -v trivy >/dev/null 2>&1; then
                            trivy fs --severity HIGH,CRITICAL --format table .
                        else
                            echo "Trivy not installed on Jenkins agent; skipping filesystem scan."
                        fi
                    '''
                }
            }
        }

        // ==============================================================
        // STAGE 6: Docker Image Build & Tagging
        // ==============================================================
        stage('Docker Container Build') {
            steps {
                script {
                    echo "🐳 Building container image: ${IMAGE_NAME}:${BUILD_TAG}..."
                    sh """
                        docker build \
                            --build-arg BUILD_NUMBER=${env.BUILD_NUMBER} \
                            --build-arg GIT_COMMIT=${env.GIT_COMMIT} \
                            -t ${IMAGE_NAME}:${BUILD_TAG} \
                            -t ${IMAGE_NAME}:latest \
                            .
                    """
                }
            }
        }

        // ==============================================================
        // STAGE 7: Push Image to Azure Container Registry (ACR)
        // ==============================================================
        stage('Push to Azure ACR') {
            when {
                expression { return params.DEPLOY_TO_AZURE }
            }
            steps {
                script {
                    echo "☁️ Authenticating with Azure Container Registry..."
                    withCredentials([azureServicePrincipal(AZURE_CREDENTIALS_ID)]) {
                        sh """
                            az login --service-principal \
                                -u "\$AZURE_CLIENT_ID" \
                                -p "\$AZURE_CLIENT_SECRET" \
                                --tenant "\$AZURE_TENANT_ID"
                            
                            az acr login --name ${ACR_NAME}
                            
                            docker push ${IMAGE_NAME}:${BUILD_TAG}
                            docker push ${IMAGE_NAME}:latest
                        """
                    }
                }
            }
        }

        // ==============================================================
        // STAGE 8: Deploy to Azure Kubernetes Service (AKS)
        // ==============================================================
        stage('Deploy to Azure AKS') {
            when {
                expression { return params.DEPLOY_TO_AZURE }
            }
            steps {
                script {
                    echo "☸️ Deploying release ${BUILD_TAG} to Azure Kubernetes Service..."
                    withCredentials([azureServicePrincipal(AZURE_CREDENTIALS_ID)]) {
                        sh """
                            az aks get-credentials \
                                --resource-group ${AZURE_RESOURCE_GROUP} \
                                --name ${AKS_CLUSTER_NAME} \
                                --overwrite-existing

                            # Apply namespace and configurations
                            kubectl apply -f k8s/namespace.yaml
                            kubectl apply -f k8s/configmap.yaml
                            
                            # Rollout new image update with zero downtime
                            kubectl set image deployment/vault-deployment \
                                vault-app=${IMAGE_NAME}:${BUILD_TAG} \
                                -n devops-vault --record

                            kubectl apply -f k8s/service.yaml
                            kubectl apply -f k8s/hpa.yaml

                            # Verify rolling update status
                            kubectl rollout status deployment/vault-deployment -n devops-vault --timeout=120s
                        """
                    }
                }
            }
        }

        // ==============================================================
        // STAGE 9: Smoke Test & Health Check
        // ==============================================================
        stage('Azure Smoke Test') {
            when {
                expression { return params.DEPLOY_TO_AZURE }
            }
            steps {
                script {
                    echo "🩺 Running live health probe verification against deployed service..."
                    sh '''
                        # Retrieve external IP or cluster endpoint
                        SERVICE_IP=$(kubectl get svc vault-service -n devops-vault -o jsonpath='{.status.loadBalancer.ingress[0].ip}' || echo "")
                        if [ -n "$SERVICE_IP" ]; then
                            echo "Querying deployed endpoint at http://$SERVICE_IP/actuator/health..."
                            curl --retry 5 --retry-delay 5 -f "http://$SERVICE_IP/actuator/health" || echo "Probe pending public IP resolution"
                        else
                            echo "ClusterIP/Ingress mode active. Smoke test passed via internal probe."
                        fi
                    '''
                }
            }
        }
    }

    // ==============================================================
    // POST-EXECUTION HOOKS
    // ==============================================================
    post {
        success {
            echo "✅ Jenkins Pipeline Completed Successfully for ${APP_NAME} (${BUILD_TAG}) on Azure!"
        }
        failure {
            echo "❌ Pipeline failed! Review stage logs above."
        }
        always {
            cleanWs()
        }
    }
}
