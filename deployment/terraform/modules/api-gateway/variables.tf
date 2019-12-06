variable "container-tag" {default = "development"}
variable "image-pull-secret" {}
variable "namespace" {default = "default"}
variable "external-ip-name" {default = "api-gateway-external-ip"}

variable "archivist_host" {}
variable "auth_server_host" {}
