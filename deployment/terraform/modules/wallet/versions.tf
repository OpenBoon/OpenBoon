
terraform {
  required_version = ">= 0.13"
  required_providers {
    google = {
      source = "hashicorp/google"
    }
    google-beta = {
      source = "hashicorp/google-beta"
    }
    kubernetes = {
      source = "hashicorp/kubernetes"
      version = "2.0.2"
    }
    random = {
      source  = "hashicorp/random"
      version = ">= 2.2.0"
    }
  }
}
