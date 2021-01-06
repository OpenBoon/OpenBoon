variable "project" {
}

variable "zone" {
}

variable "container-cluster-name" {
}

variable "image-pull-secret" {
  sensitive = true
}

variable "data-bucket-name" {
}

variable "redis-host" {
}

variable "container-tag" {
  default = "latest"
}

variable "machine-type" {
  default = "custom-6-18176"
}

variable "memory-request" {
  default = "4Gi"
}

variable "memory-limit" {
  default = "8Gi"
}

variable "cpu-request" {
  default = 2
}

variable "cpu-limit" {
  default = 3
}

variable "minimum-nodes" {
  default = 1
}

variable "maximum-nodes" {
  default = 3
}

variable "minimum-replicas" {
  default = 1
}

variable "maximum-replicas" {
  default = 2
}

variable "rollout-strategy" {
  default = "RollingUpdate"
}

variable "ip-address" {
  default = "10.3.240.105"
}

variable "namespace" {
  default = "default"
}

variable "node-pool-name" {
  default = "officer"
}

