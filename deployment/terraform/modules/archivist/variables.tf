variable "project" {}
variable "region" {}
variable "image-pull-secret" {}
variable "sql-instance-name" {}
variable "sql-connection-name" {}
variable "sql-service-account-key" {}
variable "inception-key-b64" {}
variable "minio-access-key" {}
variable "minio-secret-key" {}

variable "data-bucket-name" {default = "archivist-data"}
variable "container-tag" {default = "development"}
variable "minimum-replicas" {default = 2}
variable "maximum-replicas" {default = 2}
variable "rollout-strategy" {default = "RollingUpdate"}
variable "ip-address" {default = "10.3.240.100"}
variable "database-name" {default = "zorroa"}
variable "database-user" {default = "zorroa"}
variable "namespace" {default = "default"}
variable "minio-url" {default = "http://10.3.240.102:9000"}
