variable "inception-key-b64" {
  sensitive = true
}

variable "project" {
}

variable "container-tag" {
}

variable "image-pull-secret" {
  sensitive = true
}

variable "zmlp-api-url" {
}

variable "monitor-password" {
  sensitive = true
}

variable "collection-interval" {
  default = "60"
}

variable "namespace" {
  default = "default"
}
