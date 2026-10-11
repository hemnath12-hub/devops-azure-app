# ==============================================================
# Terraform Infrastructure as Code for Azure DevOps Deployment
# Provisions: Resource Group, Storage Account (Blobs), ACR, AKS Cluster
# ==============================================================

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.90.0"
    }
  }
}

provider "azurerm" {
  features {}
}

# 1. Azure Resource Group
resource "azurerm_resource_group" "rg" {
  name     = var.resource_group_name
  location = var.azure_region

  tags = {
    Environment = var.environment
    Project     = "SecureDocumentVault"
    ManagedBy   = "Terraform"
  }
}

# 2. Azure Storage Account (for Document & Blob Vault)
resource "azurerm_storage_account" "storage" {
  name                     = var.storage_account_name
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = azurerm_resource_group.rg.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  min_tls_version          = "TLS1_2"

  blob_properties {
    versioning_enabled = true
    delete_retention_policy {
      days = 7
    }
  }

  tags = azurerm_resource_group.rg.tags
}

# Blob Container for documents
resource "azurerm_storage_container" "documents" {
  name                  = var.storage_container_name
  storage_account_name  = azurerm_storage_account.storage.name
  container_access_type = "private"
}

# 3. Azure Container Registry (ACR)
resource "azurerm_container_registry" "acr" {
  name                = var.acr_name
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
  sku                 = "Standard"
  admin_enabled       = true

  tags = azurerm_resource_group.rg.tags
}

# 4. Azure Kubernetes Service (AKS) Cluster
resource "azurerm_kubernetes_cluster" "aks" {
  name                = var.aks_cluster_name
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  dns_prefix          = "${var.aks_cluster_name}-dns"

  default_node_pool {
    name       = "systempool"
    node_count = var.aks_node_count
    vm_size    = var.aks_vm_size
    os_disk_size_gb = 50
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin    = "kubenet"
    load_balancer_sku = "standard"
  }

  tags = azurerm_resource_group.rg.tags
}

# 5. Role Assignment: Grant AKS Kubelet Identity AcrPull permissions on ACR
resource "azurerm_role_assignment" "aks_acr_pull" {
  principal_id                     = azurerm_kubernetes_cluster.aks.kubelet_identity[0].object_id
  role_definition_name             = "AcrPull"
  scope                            = azurerm_container_registry.acr.id
  skip_service_principal_aad_check = true
}

# 6. Role Assignment: Grant AKS Kubelet Identity Blob Data Contributor on Storage
resource "azurerm_role_assignment" "aks_storage_contributor" {
  principal_id                     = azurerm_kubernetes_cluster.aks.kubelet_identity[0].object_id
  role_definition_name             = "Storage Blob Data Contributor"
  scope                            = azurerm_storage_account.storage.id
  skip_service_principal_aad_check = true
}
