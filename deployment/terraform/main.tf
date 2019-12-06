## Configuration
terraform {
  backend "gcs" {
    project = "zorroa-deploy"
    bucket = "zorroa-deploy-state"
    prefix = "terraform/state"
  }
}

## Providers
provider "google" {
  credentials = "${var.terraform-credentials}"
  project = "${var.project}"
  region = "${local.region}"
  zone = "${local.zone}"
  version = ">= 2.11.0"
}

provider "google-beta" {
  credentials = "${var.terraform-credentials}"
  project = "${var.project}"
  region = "${local.region}"
  zone = "${local.zone}"
  version = ">= 2.11.0"
}

provider "kubernetes" {
  host     = "${module.gke-cluster.endpoint}"
  username = "${module.gke-cluster.username}"
  password = "${module.gke-cluster.password}"
  client_certificate     = "${module.gke-cluster.client_certificate}"
  client_key             = "${module.gke-cluster.client_key}"
  cluster_ca_certificate = "${module.gke-cluster.cluster_ca_certificate}"
  version = ">= 1.6.0"
}

## GKE Cluster.
module "gke-cluster" {
  source = "./modules/gke-cluster"
  zone = "${local.zone}"
  initial-node-count = "${var.initial-node-count}"
}

## Postgres DB
module "postgres" {
  source = "./modules/postgres"
  project = "${var.project}"
  region = "${local.region}"
  sql-tier = "${var.sql-tier}"
}

## Docker Hub credentials for pulling images.
locals {
  dockerconfigjson = {
    auths = {
      "https://index.docker.io/v1/" = {
        email    = "${var.docker-email}"
        username = "${var.docker-username}"
        password = "${var.docker-password}"
        auth = "${base64encode(format("%s:%s", var.docker-username, var.docker-password))}"
      }
    }
  }
}

resource "kubernetes_secret" "dockerhub" {
  metadata {
    name = "dockerhubcreds"
  }
  data {
    ".dockerconfigjson" = "${ jsonencode(local.dockerconfigjson) }"
  }
  type = "kubernetes.io/dockerconfigjson"
}

## Wallet
module "wallet" {
  source = "./modules/wallet"
  project = "${var.project}"
  container-cluster-name = "${module.gke-cluster.name}"
  container-tag = "${var.container-tag}"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
  pg_host = "${module.postgres.ip-address}"
  sql-instance-name = "${module.postgres.instance-name}"
  sql-service-account-key = "${module.postgres.sql-service-account-key}"
  sql-connection-name = "${module.postgres.connection-name}"
}

## Redis DB
module "redis" {
  source = "./modules/redis"
}

## Elasticsearch DB
module "elasticsearch" {
  source = "./modules/elasticsearch"
  container-cluster-name = "${module.gke-cluster.name}"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
}

## Archivist
module "archivist" {
  source = "./modules/archivist"
  project = "${var.project}"
  region = "${local.region}"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
  sql-service-account-key = "${module.postgres.sql-service-account-key}"
  sql-instance-name = "${module.postgres.instance-name}"
  sql-connection-name = "${module.postgres.connection-name}"
}

