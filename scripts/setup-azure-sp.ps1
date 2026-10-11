<#
.SYNOPSIS
  Automated Azure Service Principal Setup Script for Jenkins CI/CD
.DESCRIPTION
  Creates an Azure Active Directory Service Principal with Contributor role,
  queries subscription and tenant IDs, and formats the credentials for Jenkins.
#>

param(
    [string]$SubscriptionId = "",
    [string]$SpName = "jenkins-vault-ci-sp",
    [string]$ResourceGroup = "devops-vault-rg"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Azure Service Principal Setup for Jenkins & DevOps" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Check Azure CLI login
$account = az account show 2>$null | ConvertFrom-Json
if (-not $account) {
    Write-Host "Please login to Azure first via: az login" -ForegroundColor Yellow
    exit 1
}

if (-not $SubscriptionId) {
    $SubscriptionId = $account.id
}

Write-Host "Using Azure Subscription: $($account.name) ($SubscriptionId)" -ForegroundColor Green

# Create Service Principal
Write-Host "Creating Service Principal '$SpName'..." -ForegroundColor Yellow
$spOutput = az ad sp create-for-rbac `
    --name $SpName `
    --role "Contributor" `
    --scopes "/subscriptions/$SubscriptionId" `
    --output json | ConvertFrom-Json

if ($spOutput) {
    Write-Host "`n✅ Service Principal Created Successfully!" -ForegroundColor Green
    Write-Host "----------------------------------------------------------"
    Write-Host "Add these credentials to Jenkins Credentials Store:" -ForegroundColor Cyan
    Write-Host "Kind: Microsoft Azure Service Principal"
    Write-Host "ID:   azure-sp-credentials"
    Write-Host ""
    Write-Host "Subscription ID: $SubscriptionId"
    Write-Host "Client ID:       $($spOutput.appId)"
    Write-Host "Client Secret:   $($spOutput.password)"
    Write-Host "Tenant ID:       $($spOutput.tenant)"
    Write-Host "----------------------------------------------------------"
} else {
    Write-Host "Failed to create Service Principal." -ForegroundColor Red
}
