output "vision-region" {
  value = azurerm_cognitive_account.vision.location
}

output "vision-endpoint" {
  value = azurerm_cognitive_account.vision.endpoint
}

output "vision-key" {
  value     = azurerm_cognitive_account.vision.primary_access_key
  sensitive = true
}
