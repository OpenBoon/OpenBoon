variable "project" {}
variable "zone" {}
variable "container-cluster-name" {}
variable "image-pull-secret" {}
variable "minio-url" {}
variable "minio-access-key" {}
variable "minio-secret-key" {}

variable "container-tag" {default = "development"}
variable "machine-type" {default = "custom-6-18176"}
variable "memory-request" {default = "4Gi"}
variable "memory-limit" {default = "8Gi"}
variable "cpu-request" {default = 2.0}
variable "cpu-limit" {default = 3.0}
variable "minimum-nodes" {default = 1}
variable "maximum-nodes" {default = 3}
variable "minimum-replicas" {default = 1}
variable "maximum-replicas" {default = 2}
variable "rollout-strategy" {default = "RollingUpdate"}
variable "ip-address" {default = "10.3.240.105"}
variable "namespace" {default = "default"}
variable "node-pool-name" {default = "officer"}
