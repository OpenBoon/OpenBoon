variable "project" {}
variable "pg_host" {}
variable "sql-instance-name" {}
variable "sql-service-account-key" {}
variable "sql-connection-name" {}
variable "container-cluster-name" {}
variable "image-pull-secret" {}

variable "container-tag" {default = "development"}
variable "namespace" {default = "default"}
variable "external-ip-name" {default = "curator-external-ip"}
variable "database-name" {default = "wallet"}
variable "database-user" {default = "wallet"}

