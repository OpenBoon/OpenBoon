
variable "sql-instance-name" {
}

variable "sql-connection-name" {
}

variable "image-pull-secret" {
  sensitive = true
}

variable "environment" {
}

variable "container-tag" {
  default = "latest"
}

variable "namespace" {
  default = "default"
}

variable "database-name" {
  default = "metrics"
}

variable "database-user" {
  default = "metrics"
}

variable "browsable" {
  default = "false"
}

variable "debug" {
  default = "false"
}

variable "ip-address" {
  default = "10.3.240.109"
}

variable "superuser-email" {
  default = "admin@example.com"
}

variable "superuser-password" {
  default   = "admin"
  sensitive = true
}

variable "superuser-first-name" {
  default = "Admin"
}

variable "superuser-last-name" {
  default = "Adminson"
}

variable "django-log-level" {
  default = "INFO"
}

variable "log-requests" {
  default = "false"
}

