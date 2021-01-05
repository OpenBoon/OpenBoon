variable "container-cluster-name" {
}

variable "image-pull-secret" {
  sensitive = true
}

variable "project" {
}

variable "country" {
}

variable "log-bucket-name" {}

variable "container-tag" {
  default = "latest"
}

variable "namespace" {
  default = "default"
}

variable "node-pool-name" {
  default = "elasticsearch"
}

variable "storage-class-name" {
  default = "elasticsearch"
}

variable "ip-address" {
  default = "10.3.240.106"
}
