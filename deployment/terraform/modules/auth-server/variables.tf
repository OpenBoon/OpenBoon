variable "image-pull-secret" {
  sensitive = true
}

variable "sql-instance-name" {
}

variable "sql-connection-name" {
}

variable "sql-service-account-key-date" {
}

variable "inception-key-b64" {
  sensitive = true
}

variable "system-bucket" {
}

variable "container-cluster-name" {
}

variable "container-tag" {
  default = "latest"
}

variable "namespace" {
  default = "default"
}

variable "ip-address" {
  default = "10.3.240.101"
}

variable "database-name" {
  default = "zorroa"
}

variable "database-user" {
  default = "zorroa"
}

