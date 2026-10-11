# Secure Asset & Document Vault (DevOps Architecture)

An enterprise-grade, cloud-native microservice built with **Java 17 & Spring Boot 3**, packaged and tested with **Gradle**, continuously integrated and delivered via a **Declarative Jenkinsfile Pipeline**, and deployed to **Microsoft Azure** (Azure Blob Storage, Azure Container Registry, and Azure Kubernetes Service) with **Terraform** Infrastructure as Code.

---

## Architecture Overview

```
                      +---------------------------------------+
                      |       Developer / Git Repository      |
                      +---------------------------------------+
                                          |
                                          v (git push / webhook)
                      +---------------------------------------+
                      |       Jenkins CI/CD Orchestrator      |
                      +---------------------------------------+
                        |  1. Gradle Build & Compile
                        |  2. JUnit 5 Tests & JaCoCo Coverage
                        |  3. SonarQube Quality Gate
                        |  4. Trivy Container Vulnerability Scan
                        |  5. Docker Multi-Stage Image Build
                        +-------------------+------------------+
                                            |
                         (docker push)      |     (kubectl rollout)
                                            v
+-----------------------------+     +-------------------------------+
| Azure Container Reg (ACR)   | <---| Azure Kubernetes Service (AKS) |
| devopsvaultacr.azurecr.io   |     | Pods with Actuator probes     |
+-----------------------------+     +---------------+---------------+
                                                    |
                                                    v (Blob SDK)
                                    +-------------------------------+
                                    | Azure Storage Account         |
                                    | Container: documents (Blobs)  |
+-----------------------------+     +-------------------------------+
| HashiCorp Terraform (IaC)   | ----> Provisions all Azure Resources
+-----------------------------+
```

---

## Technology Stack

| Domain | Technology | Purpose |
| :--- | :--- | :--- |
| **Backend Service** | Java 17, Spring Boot 3.3.4 | Core REST microservice & business logic |
| **Cloud Storage** | Azure Blob Storage SDK (`12.28.0`) | Encrypted, highly-available file/blob persistence |
| **Telemetry & Health** | Spring Boot Actuator, Prometheus | Liveness/Readiness probes (`/healthz`) & metrics |
| **Web Portal UI** | Modern Vanilla JS, CSS Glassmorphism | Real-time vault dashboard & DevOps status |
| **Build & Test Tool** | Gradle 8.7, JUnit 5, JaCoCo | Compilation, dependency resolution & coverage |
| **Containerization** | Docker (Multi-stage, Distroless/Alpine) | Secure runtime image with non-root UID 10001 |
| **Local Cloud Emulator**| Microsoft Azurite | Offline Azure Blob Storage emulation |
| **CI/CD Pipeline** | Jenkins (Declarative Pipeline) | Multi-stage build, scan, push & zero-downtime deploy |
| **Cloud Infrastructure**| Terraform (azurerm ~> 3.90) | Automated Azure RG, ACR, AKS & Storage provisioning |
| **Orchestration** | Kubernetes (AKS) | Scalable container orchestration, Ingress & HPA |

---

## Project Structure

```
devops-azure-app/
├── Jenkinsfile                  # Enterprise Declarative CI/CD pipeline
├── Dockerfile                   # Multi-stage Docker build (Alpine JRE, non-root)
├── docker-compose.yml           # Local orchestration (App + Azurite emulator)
├── build.gradle                 # Gradle dependencies, Spring Boot & JaCoCo setup
├── settings.gradle              # Gradle root project definition
├── gradlew / gradlew.bat        # Cross-platform Gradle wrapper scripts
├── gradle/wrapper/              # Pinned Gradle 8.7 distribution
├── src/
│   ├── main/
│   │   ├── java/com/devops/vault/
│   │   │   ├── DocumentVaultApplication.java
│   │   │   ├── controller/      # REST APIs (DocumentController, SystemInfoController)
│   │   │   ├── model/           # Domain models (Document, StorageStats, Responses)
│   │   │   └── service/         # StorageService (AzureBlobStorageService, LocalStorageService)
│   │   └── resources/
│   │       ├── application.yml  # Actuator, Azure & file size configuration
│   │       └── static/          # Web dashboard (index.html, style.css, app.js)
│   └── test/java/com/devops/vault/
│       ├── DocumentVaultApplicationTests.java
│       └── DocumentServiceTest.java
├── terraform/                   # Azure Infrastructure as Code
│   ├── main.tf                  # RG, Storage Account, ACR, AKS cluster
│   ├── variables.tf             # Configurable Azure inputs
│   ├── outputs.tf               # Connection strings, cluster credentials, ACR URL
│   └── terraform.tfvars.example
├── k8s/                         # Kubernetes deployment manifests
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml          # RollingUpdate, resource limits, Actuator probes
│   ├── service.yaml             # LoadBalancer / ClusterIP
│   ├── ingress.yaml             # Ingress routing
│   └── hpa.yaml                 # Horizontal Pod Autoscaler (2-8 replicas)
└── scripts/                     # DevOps automation scripts
    ├── setup-azure-sp.ps1       # One-click Azure Service Principal setup (PowerShell)
    └── setup-azure-sp.sh        # One-click Azure Service Principal setup (Bash)
```

---

## 1. Quickstart: Local Development

### Option A: Local Execution with Gradle
Ensure Java 17+ is installed:
```powershell
# Run the application (uses local disk storage fallback by default)
./gradlew bootRun

# Run unit tests and generate JaCoCo coverage report
./gradlew test jacocoTestReport
```
Open your browser at: `http://localhost:8080`

### Option B: Local Multi-Container with Azurite (Offline Azure Emulator)
Run the application connected to Microsoft's official Azure Storage Emulator:
```powershell
docker-compose up --build
```
The application will connect to Azurite on port `10000`, testing real Azure Blob Storage SDK calls completely offline without an Azure cloud bill.

---

## 2. Infrastructure as Code: Provisioning Azure via Terraform

1. Login to Azure:
   ```bash
   az login
   ```
2. Navigate to the terraform directory:
   ```bash
   cd terraform
   cp terraform.tfvars.example terraform.tfvars
   ```
3. Initialize and apply:
   ```bash
   terraform init
   terraform plan -out=tfplan
   terraform apply tfplan
   ```
4. Terraform outputs the ACR login server and AKS cluster connect command:
   ```bash
   az aks get-credentials --resource-group devops-vault-rg --name devops-vault-aks
   ```

---

## 3. Jenkins CI/CD Setup & Execution

### Prerequisites in Jenkins
Ensure the following plugins are installed in Jenkins:
- **Pipeline**
- **Docker Pipeline**
- **Azure CLI Plugin** / **Azure Credentials Plugin**
- **JUnit Plugin**
- **JaCoCo Plugin**

### Adding Azure Credentials in Jenkins
Run the automated helper script to create the Service Principal:
```powershell
# Windows PowerShell:
.\scripts\setup-azure-sp.ps1

# Linux / Mac / Jenkins Agent:
./scripts/setup-azure-sp.sh
```
In Jenkins:
1. Navigate to **Manage Jenkins** → **Credentials** → **System** → **Global credentials**.
2. Click **Add Credentials**:
   - **Kind**: Microsoft Azure Service Principal
   - **ID**: `azure-sp-credentials`
   - Fill in: `Subscription ID`, `Client ID`, `Client Secret`, `Tenant ID`.

### Creating the Jenkins Pipeline
1. In Jenkins, click **New Item** → select **Pipeline**.
2. Under **Pipeline Definition**, select **Pipeline script from SCM** (Git).
3. Set your repository URL and specify the script path as `Jenkinsfile`.
4. Click **Build with Parameters** and choose your target environment (`dev`, `staging`, `prod`).

---

## 4. API & Health Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/documents/upload` | Upload document with tags (computes SHA-256 fingerprint) |
| `GET` | `/api/v1/documents` | List documents with query and tag filtering |
| `GET` | `/api/v1/documents/{id}/download` | Stream download with checksum header |
| `DELETE` | `/api/v1/documents/{id}` | Delete document from vault and Azure Blob Storage |
| `GET` | `/api/v1/documents/stats` | Storage metrics, total footprint & provider status |
| `GET` | `/api/v1/system/info` | DevOps metadata (Build #, Git commit, Azure status) |
| `GET` | `/actuator/health` | Kubernetes Liveness & Readiness probe status |
| `GET` | `/actuator/prometheus` | Prometheus-formatted metrics scrape target |

---

## 5. Security & Production Best Practices

- **Non-Root Container**: The Docker image creates and runs under UID `10001` (`vaultuser`), preventing container breakout vulnerabilities.
- **Integrity Fingerprints**: Every uploaded file is hashed with **SHA-256** upon ingestion, ensuring tamper-evident storage.
- **Zero-Downtime Rollouts**: Kubernetes `RollingUpdate` with `maxSurge: 1` and `maxUnavailable: 0` ensures 100% uptime during deployments.
- **Auto-Scaling (HPA)**: Kubernetes dynamically scales pods from 2 to 8 replicas based on CPU and memory thresholds.
