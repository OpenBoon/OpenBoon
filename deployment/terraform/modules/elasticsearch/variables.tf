variable "container-cluster-name" {
}

variable "image-pull-secret" {
}

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

