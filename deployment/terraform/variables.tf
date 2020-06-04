## Required Variables.
variable "project" {
  description = "Name of the GCP project."
}

variable "environment" {
  description = "Name of the deployment environment. Used for things like sentry reporting."
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

variable "wallet-domain" {
  description = "Fully qualified domain name for the wallet server."
}

variable "zmlp-domain" {
  description = "Domain name of the zmlp api."
}

variable "container-tag" {
  description = "Tag to use for all zvi service docker contianers."
  default = "stable"
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

variable "deploy-marketplace-integration" {
  description = "If this variable exists the GCP Marketplace integration will be deployed."
  default = ""
}

variable "marketplace-credentials" {
  description = "GCP Service Account JSON key to use with the GCP Procurement API."
  default = ""
}

variable "wallet-debug" {
  description = "Turns Debug mode on for the Console/Wallet"
  default = "false"
}

variable "wallet-browsable-api" {
  description = "Turns on the Wallet/Console browsable API for an environment."
  default = "false"
}

## Generated Variables
locals {
  region = "${var.country}-${var.region}"
  zone   = "${var.country}-${var.region}-${var.zone}"
}

