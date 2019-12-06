variable "container-cluster-name" {}
variable "container-tag" {default = "development"}
variable "image-pull-secret" {}
variable "es-cluster-size" {default = 1}
variable "namespace" {default = "default"}
variable "node-pool-name" {default = "elasticsearch"}
variable "storage-class-name" {default = "elasticsearch"}
