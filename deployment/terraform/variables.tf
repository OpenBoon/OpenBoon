## Required Variables.
variable "project" {
  description = "Name of the GCP project."
}

variable "environment" {
  description = "Name of the deployment environment. Used for things like sentry reporting."
}

variable "terraform-credentials" {
  description = "Contents of a GCP credential json file to use for creating resources."
}

variable "azure-subscription-id" {
  description = "Azure subscription ID to be used with Azure provider."
}

variable "azure-client-id" {
  description = "Client ID from the Azure service principal to be used with the Azure provider."
}

variable "azure-tenant-id" {
  description = "Tenant/directory ID of the Azure service principal to be used with the Azure provider."
}

variable "azure-client-secret" {
  description = "Secret key generated for the Azure service principal to be used with the Azure provider."
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

variable "clarifai-key" {
  description = "Secret key to allow access to the Clarifai API."
}

variable "aws-key" {
  description = "Secret key to allow access to the AWS ML API."
}

variable "aws-secret" {
  description = "Secret to allow access to the AWS ML API."
}

variable "aws-region" {
  description = "Region where AWS resources will be created."
  default     = "us-east-2"
}

variable "analyst-memory-request" {
  description = "Memory request for Analyst pods."
  default     = "2Gi"
}

variable "analyst-memory-limit" {
  description = "Memory limit for Analyst pods."
  default     = "3Gi"
}

variable "analyst-cpu-request" {
  description = "CPU request for Analyst pods."
  default     = "0.5"
}

variable "analyst-cpu-limit" {
  description = "CPU limit for Analyst pods."
  default     = "1"
}

variable "analyst-machine-type" {
  description = "Machine type for the Analysts' node pool."
  default     = "custom-6-18176"
}

variable "container-tag" {
  description = "Tag to use for all zvi service docker contianers."
  default     = "stable"
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

variable "sql-tier" {
  description = "Machine tier to use for the Postgres server."
  default     = "db-custom-1-4096"
}

variable "deploy-marketplace-integration" {
  description = "If this variable exists the GCP Marketplace integration will be deployed."
  default     = ""
}

variable "marketplace-credentials" {
  description = "GCP Service Account JSON key to use with the GCP Procurement API."
  default     = ""
}

variable "wallet-debug" {
  description = "Turns Debug mode on for the Console/Wallet"
  default     = "false"
}

variable "wallet-browsable-api" {
  description = "Turns on the Wallet/Console browsable API for an environment."
  default     = "false"
}

variable "wallet-superadmin" {
  description = "Turns on a more advanced Django Admin view at /superadmin."
  default     = "false"
}

variable "wallet-use-model-ids-for-label-filters" {
  description = "Feature flag for label filters return in the Console"
  default     = "false"
}

## Generated Variables
locals {
  region = "${var.country}-${var.region}"
  zone   = "${var.country}-${var.region}-${var.zone}"
}

