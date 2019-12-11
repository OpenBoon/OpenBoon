variable "project" {}
variable "config-bucket-name" {default = "archivist-config"}
variable "data-bucket-name" {default = "archivist-data"}
variable "region" {}
variable "image-pull-secret" {}
variable "sql-instance-name" {}
variable "sql-connection-name" {}
variable "sql-service-account-key" {}
variable "container-tag" {default = "development"}
variable "monitor-password" {default = "5tgb%TGB"}
variable "config-properties" {default = ""}
variable "extensions" {default = ""}
variable "minimum-replicas" {default = 2}
variable "maximum-replicas" {default = 5}
variable "minimum-nodes" {default = 1}
variable "maximum-nodes" {default = 5}
variable "rollout-strategy" {default = "RollingUpdate"}
variable "ip-address" {default = "10.3.240.100"}
variable "database-name" {default = "zorroa"}
variable "database-user" {default = "zorroa"}
variable "namespace" {default = "default"}
variable "node-pool-name" {default = "archivist"}
variable "redis-host" {default = "10.3.240.104"}
