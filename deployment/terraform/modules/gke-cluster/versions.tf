
terraform {
  required_version = ">= 0.13"
  required_providers {
    google = {
      source = "hashicorp/google"
    }
    google-beta = {
      source = "hashicorp/google-beta"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
