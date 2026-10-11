variable "azure_region" {
  type        = string
  description = "Target Azure Region"
  default     = "eastus"
}

variable "resource_group_name" {
  type        = string
  description = "Azure Resource Group Name"
  default     = "devops-vault-rg"
}

variable "environment" {
  type        = string
  description = "Environment tier (dev, staging, prod)"
  default     = "staging"
}

variable "storage_account_name" {
  type        = string
  description = "Globally unique name for the Azure Storage Account (lowercase, numbers only, 3-24 chars)"
  default     = "devopsvaultstore01"
}

variable "storage_container_name" {
  type        = string
  description = "Blob container name for storing uploaded documents"
  default     = "documents"
}

variable "acr_name" {
  type        = string
  description = "Azure Container Registry Name (alphanumeric only)"
  default     = "devopsvaultacr"
}

variable "aks_cluster_name" {
  type        = string
  description = "Azure Kubernetes Service Cluster Name"
  default     = "devops-vault-aks"
}

variable "aks_node_count" {
  type        = number
  description = "Initial number of AKS worker nodes"
  default     = 2
}

variable "aks_vm_size" {
  type        = string
  description = "Virtual Machine size for AKS worker nodes"
  default     = "Standard_B2s"
}
