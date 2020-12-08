variable "project" {
}

variable "country" {
}

variable "image-pull-secret" {
}

variable "sql-instance-name" {
}

variable "sql-connection-name" {
}

variable "sql-service-account-key" {
}

variable "inception-key-b64" {
}

variable "system-bucket" {
}

variable "container-cluster-name" {
}

variable "analyst-shared-key" {
}

variable "es-backup-bucket-name" {
}

variable "log-bucket-name" {}

variable "data-bucket-name" {
  default = "archivist-data"
}

variable "container-tag" {
  default = "latest"
}

variable "rollout-strategy" {
  default = "RollingUpdate"
}

variable "ip-address" {
  default = "10.3.240.100"
}

variable "database-name" {
  default = "zorroa"
}

variable "database-user" {
  default = "zorroa"
}

variable "namespace" {
  default = "default"
}

variable "auth-server-url" {
  default = "http://10.3.240.101"
}

variable "elasticsearch-url" {
  default = "http://10.3.240.106"
}

