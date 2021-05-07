variable "project" {
}

variable "pg_host" {
}

variable "pg_password" {
  sensitive = true
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

variable "database-name" {
  default = "wallet"
}

variable "database-user" {
  default = "wallet"
}

variable "inception-key-b64" {
  sensitive = true
}

