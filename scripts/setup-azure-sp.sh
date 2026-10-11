#!/usr/bin/env bash
# ==============================================================
# Azure Service Principal Setup Script for Jenkins CI/CD (Bash)
# ==============================================================

set -euo pipefail

SP_NAME=${1:-"jenkins-vault-ci-sp"}
SUBSCRIPTION_ID=${2:-$(az account show --query id -o tsv 2>/dev/null || echo "")}

echo "=========================================================="
echo "Azure Service Principal Setup for Jenkins & DevOps"
echo "=========================================================="

if [ -z "$SUBSCRIPTION_ID" ]; then
    echo "⚠️ Please login to Azure first: az login"
    exit 1
fi

echo "Using Azure Subscription ID: $SUBSCRIPTION_ID"
echo "Creating Service Principal: $SP_NAME..."

SP_JSON=$(az ad sp create-for-rbac \
    --name "$SP_NAME" \
    --role "Contributor" \
    --scopes "/subscriptions/$SUBSCRIPTION_ID" \
    --output json)

APP_ID=$(echo "$SP_JSON" | grep -o '"appId": "[^"]*' | cut -d'"' -f4)
PASSWORD=$(echo "$SP_JSON" | grep -o '"password": "[^"]*' | cut -d'"' -f4)
TENANT_ID=$(echo "$SP_JSON" | grep -o '"tenant": "[^"]*' | cut -d'"' -f4)

echo ""
echo "✅ Service Principal Created Successfully!"
echo "----------------------------------------------------------"
echo "Configure in Jenkins (Manage Jenkins -> Credentials):"
echo "Kind: Microsoft Azure Service Principal"
echo "ID:   azure-sp-credentials"
echo ""
echo "Subscription ID: $SUBSCRIPTION_ID"
echo "Client ID:       $APP_ID"
echo "Client Secret:   $PASSWORD"
echo "Tenant ID:       $TENANT_ID"
echo "----------------------------------------------------------"
