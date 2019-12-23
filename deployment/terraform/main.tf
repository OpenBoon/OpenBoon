## Store state in GCS. ###################################################################
terraform {
  backend "gcs" {
    project = "zorroa-deploy"
    bucket = "zorroa-deploy-state"
    prefix = "terraform/state"
  }
}

## Providers #############################################################################
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

## GCP Infrastructure ###################################################################
module "gke-cluster" {
  source = "./modules/gke-cluster"
  zone = "${local.zone}"
}

module "postgres" {
  source = "./modules/postgres"
  project = "${var.project}"
  region = "${local.region}"
}

module "redis" {
  source = "./modules/redis"
}

module "minio" {
  source = "./modules/minio"
}

## Secrets ###############################################################################
resource "random_string" "shared-key" {
  length = 64
  special = false
}

locals {
  inception-key = <<EOF
{"name": "admin-key",
    "projectId": "00000000-0000-0000-0000-000000000000",
    "keyId": "${uuid()}",
    "sharedKey": "${random_string.shared-key.result}",
    "permissions": [
        "SuperAdmin", "ProjectAdmin", "AssetsRead", "AssetsImport"
    ]
}
EOF
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


## pixelML Services ######################################################################
module "elasticsearch" {
  source = "./modules/elasticsearch"
  container-cluster-name = "${module.gke-cluster.name}"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
}

module "archivist" {
  source = "modules/archivist"
  project = "${var.project}"
  region = "${local.region}"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
  sql-service-account-key = "${module.postgres.sql-service-account-key}"
  sql-connection-name = "${module.postgres.connection-name}"
  sql-instance-name = "${module.postgres.instance-name}"
  inception-key-b64 = "${base64encode(local.inception-key)}"
  minio-access-key = "${module.minio.access-key}"
  minio-secret-key = "${module.minio.secret-key}"
}

module "auth-server" {
  source = "./modules/auth-server"
  sql-instance-name = "${module.postgres.instance-name}"
  sql-connection-name = "${module.postgres.connection-name}"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
  inception-key-b64 = "${base64encode(local.inception-key)}"
}

module "api-gateway" {
  source = "./modules/api-gateway"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
  archivist_host = "${module.archivist.ip-address}"
  auth_server_host = "${module.auth-server.ip-address}"
}

module "officer" {
  source = "./modules/officer"
  project = "${var.project}"
  zone = "${var.zone}"
  container-cluster-name = "${module.gke-cluster.name}"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
  minio-url = "http://${module.minio.ip-address}:9000"
  minio-access-key = "${module.minio.access-key}"
  minio-secret-key = "${module.minio.secret-key}"
}

module "analyst" {
  source = "./modules/analyst"
  project = "${var.project}"
  zone = "${var.zone}"
  container-cluster-name = "${module.gke-cluster.name}"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
  archivist-url = "http://${module.archivist.ip-address}"
}

module "wallet" {
  source = "./modules/wallet"
  project = "${var.project}"
  container-cluster-name = "${module.gke-cluster.name}"
  image-pull-secret = "${kubernetes_secret.dockerhub.metadata.0.name}"
  pg_host = "${module.postgres.ip-address}"
  sql-instance-name = "${module.postgres.instance-name}"
  sql-service-account-key = "${module.postgres.sql-service-account-key}"
  sql-connection-name = "${module.postgres.connection-name}"
  archivist-url = "http://${module.archivist.ip-address}"
  smtp-password = "${var.smtp-password}"
}
