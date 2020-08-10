variable "memory-request" {
  default = "1Gi"
}

variable "memory-limit" {
  default = "2Gi"
}

variable "cpu-request" {
  default = 0.5
}

variable "cpu-limit" {
  default = 1
}

variable "es-ip-address" {
  default = "10.3.240.106"
}

variable "namespace" {
  default = "default"
}
