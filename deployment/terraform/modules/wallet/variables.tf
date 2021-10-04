variable "project" {
}

variable "pg_host" {
}

variable "sql-instance-name" {
}

variable "sql-service-account-key-date" {
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

variable "smtp-host" {
}

variable "smtp-user" {
}

variable "smtp-from-email" {
}

variable "smtp-password" {
  sensitive = true
}

variable "google-oauth-client-id" {
}

variable "environment" {
}

variable "domains" {
  type = list(string)
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
