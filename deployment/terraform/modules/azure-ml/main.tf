resource "azurerm_resource_group" "zmlp" {
  name     = "zvi-${var.environment}"
  location = "Central US"
}

resource "azurerm_cognitive_account" "vision" {
  name                = "zvi-${var.environment}-vision"
  location            = azurerm_resource_group.zmlp.location
  resource_group_name = azurerm_resource_group.zmlp.name
  kind                = "ComputerVision"

  sku_name = "S1"

  tags = {
    Acceptance = "Test"
  }
}
