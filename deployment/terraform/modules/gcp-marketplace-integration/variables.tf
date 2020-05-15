variable "project" {
}

variable "pg_host" {
}

variable "pg_password" {
}

variable "marketplace-project" {
}

variable "marketplace-subscription" {
}

variable "marketplace-credentials" {
}

variable "marketplace-service-name" {
}

variable "sql-instance-name" {
}

variable "sql-service-account-key" {
}

variable "sql-connection-name" {
}

variable "image-pull-secret" {
}

variable "zmlp-api-url" {
}

variable "smtp-password" {
}

variable "google-oauth-client-id" {
}

variable "environment" {
}

variable "fqdn" {
}

variable "replicas" {
  default = 0
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
}

