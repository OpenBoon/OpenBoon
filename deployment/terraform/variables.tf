## Required Variables.
variable "project" {
  description = "Name of the GCP project."
}

variable "country" {
  description = "GCP country abbreviation (i.e. us, eu)."
  default     = "us"
}

variable "region" {
  description = "GCP region (i.e. central1, east1)."
  default     = "central1"
}

variable "zone" {
  description = "GCP zone letter (i.e. a, b)."
  default     = "a"
}

variable "terraform-credentials" {
  description = "Contents of a credential json file to use for creating resources."
}

variable "docker-username" {
  description = "Username for Docker Hub user."
}

variable "docker-password" {
  description = "Password for Docker Hub user."
}

variable "docker-email" {
  description = "Email address for Docker Hub user."
}

variable "smtp-password" {
  description = "Password for the SMTP server wallet uses to send mail."
}

variable "google-oauth-client-id" {
  description = "Client ID to enable Google OAuth based login."
}

variable "marketplace-credentials" {
  description = "GCP Service Account JSON key to use with the GCP Procurement API."
}

## Generated Variables
locals {
  region = "${var.country}-${var.region}"
  zone   = "${var.country}-${var.region}-${var.zone}"
}

