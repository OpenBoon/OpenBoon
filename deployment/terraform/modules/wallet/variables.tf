variable "project" {
}

variable "pg_host" {
}

variable "sql-instance-name" {
}

variable "sql-service-account-key" {
  sensitive = true
}

variable "sql-connection-name" {
}

variable "container-cluster-name" {
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

variable "domain" {
}

variable "marketplace-project" {
}

variable "marketplace-credentials" {
  sensitive = true
}

variable "container-tag" {
  default = "latest"
}

variable "namespace" {
  default = "default"
}

variable "external-ip-name" {
  default = "wallet-external-ip"
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

variable "browsable" {
  default = "false"
}

variable "superadmin" {
  default = "false"
}

variable "use-model-ids-for-label-filters" {
  default = "false"
}

variable "metrics-ip-address" {
  default = "metrics"
}

