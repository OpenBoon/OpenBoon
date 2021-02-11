
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
      version = "1.13.3"
    }
    random = {
      source  = "hashicorp/random"
      version = ">= 2.2.0"
    }
  }
}
