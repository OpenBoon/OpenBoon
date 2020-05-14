terraform {
  backend "remote" {
    hostname      = "app.terraform.io"
    organization  = "zorroa"

    workspaces {
      name = "zvi-production"
    }
  }
}

module "zvi-saas-deployment" {
  source = "./modules/saas-deployment"
  project = "zvi-production"
  docker-username = "zorroaadmin"
  docker-email = "software@zorroa.com"
  docker-password = var.docker-password
  google-oauth-client-id = "683985502197-b4dief89rrm8jmle59fvc6bdeesf528o"
  marketplace-credentials = var.marketplace-credentials
  smtp-password = var.smtp-password
  terraform-credentials = var.terraform-credentials
  environment = "production"
  wallet-fqdn = "https://console.zvi.zorroa.com"
  zmlp-domain = "api.zvi.zorroa.com"
}
