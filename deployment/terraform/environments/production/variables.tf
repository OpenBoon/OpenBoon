## Required Variables.
variable "terraform-credentials" {
  description = "Contents of a credential json file to use for creating resources."
}


variable "docker-password" {
  description = "Password for Docker Hub user."
}

variable "smtp-password" {
  description = "Password for the SMTP server wallet uses to send mail."
}

variable "marketplace-credentials" {
  description = "GCP Service Account JSON key to use with the GCP Procurement API."
}
