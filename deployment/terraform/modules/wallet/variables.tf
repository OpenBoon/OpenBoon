variable "project" {}
variable "container-cluster-name" {}
variable "container-tag" {default = "development"}
variable "image-pull-secret" {}
variable "namespace" {default = "default"}
variable "external-ip-name" {default = "curator-external-ip"}
variable "database-name" {default = "wallet"}
variable "database-user" {default = "wallet"}

variable "pg_host" {}
variable "sql-instance-name" {}
variable "sql-service-account-key" {}
variable "sql-connection-name" {}
