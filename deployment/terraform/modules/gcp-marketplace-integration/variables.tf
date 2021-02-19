variable "enabled" {
}

variable "project" {
}

variable "pg_host" {
}

variable "pg_password" {
  sensitive = true
}

variable "marketplace-project" {
}

variable "marketplace-subscription" {
}

variable "marketplace-credentials" {
  sensitive = true
}

variable "marketplace-service-name" {
}

variable "sql-instance-name" {
}

variable "sql-connection-name" {
}

variable "sql-service-account-key-date" {
}

variable "image-pull-secret" {
  sensitive = true
}

variable "zmlp-api-url" {
}

variable "smtp-password" {
  sensitive = true
}

variable "google-oauth-client-id" {
}

variable "environment" {
}

variable "fqdn" {
}

variable "container-tag" {
  default = "latest"
}

variable "namespace" {
  default = "default"
}

variable "external-ip-name" {
  default = "curator-external-ip"
}

variable "database-name" {
  default = "wallet"
}

variable "database-user" {
  default = "wallet"
}

variable "inception-key-b64" {
  sensitive = true
}

