variable "image-pull-secret" {
  sensitive = true
}

variable "archivist_host" {
}

variable "auth_server_host" {
}

variable "ml_bbq_host" {
}

variable "domains" {
  type = list(string)
}

variable "container-cluster-name" {
}

variable "container-tag" {
  default = "latest"
}

variable "namespace" {
  default = "default"
}

variable "external-ip-name" {
  default = "api-gateway-external-ip"
}

