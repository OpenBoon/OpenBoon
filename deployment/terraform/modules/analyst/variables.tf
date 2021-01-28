variable "project" {
}

variable "zone" {
}

variable "container-cluster-name" {
}

variable "image-pull-secret" {
  sensitive = true
}

variable "archivist-url" {
}

variable "officer-url" {
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

variable "rollout-strategy" {
  default = "RollingUpdate"
}

variable "ip-address" {
  default = "10.3.240.103"
}

variable "namespace" {
  default = "default"
}

variable "node-pool-name" {
  default = "analyst"
}
