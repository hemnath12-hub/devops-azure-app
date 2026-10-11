output "resource_group_name" {
  description = "Name of the provisioned Azure Resource Group"
  value       = azurerm_resource_group.rg.name
}

output "storage_account_name" {
  description = "Name of the Azure Storage Account"
  value       = azurerm_storage_account.storage.name
}

output "storage_primary_connection_string" {
  description = "Connection string for Azure Blob Storage (sensitive)"
  value       = azurerm_storage_account.storage.primary_connection_string
  sensitive   = true
}

output "acr_login_server" {
  description = "Azure Container Registry Login Server URL for Docker commands"
  value       = azurerm_container_registry.acr.login_server
}

output "aks_cluster_name" {
  description = "Name of the AKS cluster"
  value       = azurerm_kubernetes_cluster.aks.name
}

output "aks_connect_command" {
  description = "Azure CLI command to configure kubectl credentials"
  value       = "az aks get-credentials --resource-group ${azurerm_resource_group.rg.name} --name ${azurerm_kubernetes_cluster.aks.name}"
}
