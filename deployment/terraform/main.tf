## Store state in GCS. ###################################################################
terraform {
  backend "remote" {
    hostname      = "app.terraform.io"
    organization  = "zorroa"

    workspaces {
      name = "zvi-prod"
    }
  }
}

## Providers #############################################################################
provider "google" {
  credentials = var.terraform-credentials
  project     = var.project
  region      = "${var.country}-${var.region}"
  zone        = "${var.country}-${var.region}-${var.zone}"
  version     = ">= 3.8.0"
}

provider "google-beta" {
  credentials = var.terraform-credentials
  project     = var.project
  region      = "${var.country}-${var.region}"
  zone        = "${var.country}-${var.region}-${var.zone}"
  version     = ">= 3.8.0"
}

provider "kubernetes" {
  load_config_file       = "false"
  host                   = module.gke-cluster.endpoint
  username               = module.gke-cluster.username
  password               = module.gke-cluster.password
  client_certificate     = module.gke-cluster.client_certificate
  client_key             = module.gke-cluster.client_key
  cluster_ca_certificate = module.gke-cluster.cluster_ca_certificate
  version                = ">= 1.11.2"
}

## GCP Infrastructure ###################################################################
module "gke-cluster" {
  source = "./modules/gke-cluster"
  zone   = local.zone
}

module "postgres" {
  source  = "./modules/postgres"
  project = var.project
  region  = local.region
}

module "redis" {
  source = "./modules/redis"
}

module "minio" {
  source = "./modules/minio"
}

resource "google_storage_bucket" "system" {
  lifecycle {
    prevent_destroy = true
  }
  name = "${var.project}-zmlp-system-bucket"
}

## Secrets ###############################################################################
resource "random_string" "access-key" {
  length  = 50
  special = false
}

resource "random_string" "secret-key" {
  length  = 64
  special = false
}

locals {
  inception-key = <<EOF
{
    "name": "admin-key",
    "projectId": "00000000-0000-0000-0000-000000000000",
    "id": "f3bd2541-428d-442b-8a17-e401e5e76d06",
    "accessKey": "${random_string.access-key.result}",
    "secretKey": "${random_string.secret-key.result}",
    "permissions": [
        "ProjectFilesWrite", "SystemProjectDecrypt", "SystemManage", "SystemProjectOverride", "AssetsImport", "SystemMonitor", "ProjectManage", "ProjectFilesRead", "AssetsRead", "AssetsDelete"
    ]
}
EOF

  inception-key-b64 = base64encode(local.inception-key)
  dockerconfigjson = {
    auths = {
      "https://index.docker.io/v1/" = {
        email    = var.docker-email
        username = var.docker-username
        password = var.docker-password
        auth     = base64encode(format("%s:%s", var.docker-username, var.docker-password))
      }
    }
  }
}

resource "kubernetes_secret" "dockerhub" {
  metadata {
    name = "dockerhubcreds"
  }
  data = {
    ".dockerconfigjson" = jsonencode(local.dockerconfigjson)
  }
  type = "kubernetes.io/dockerconfigjson"
}

## Enable Google ML APIs
resource "google_project_service" "video-intelligence" {
  service            = "videointelligence.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "speech-to-text" {
  service            = "speech.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "auto-ml" {
  service            = "automl.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "model-ml" {
  service            = "ml.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "natural-language" {
  service            = "language.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "translation" {
  service            = "translate.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "vision" {
  service            = "vision.googleapis.com"
  disable_on_destroy = false
}

## ZMLP Services ######################################################################
module "elasticsearch" {
  source                 = "./modules/elasticsearch"
  project                = var.project
  country                = var.country
  container-cluster-name = module.gke-cluster.name
  image-pull-secret      = kubernetes_secret.dockerhub.metadata[0].name
  container-tag          = var.container-tag
}

module "archivist" {
  source                  = "./modules/archivist"
  project                 = var.project
  country                 = var.country
  image-pull-secret       = kubernetes_secret.dockerhub.metadata[0].name
  sql-service-account-key = module.postgres.sql-service-account-key
  sql-connection-name     = module.postgres.connection-name
  sql-instance-name       = module.postgres.instance-name
  inception-key-b64       = local.inception-key-b64
  minio-access-key        = module.minio.access-key
  minio-secret-key        = module.minio.secret-key
  system-bucket           = google_storage_bucket.system.name
  container-cluster-name  = module.gke-cluster.name
  analyst-shared-key      = module.analyst.shared-key
  container-tag           = var.container-tag
  es-backup-bucket-name   = module.elasticsearch.backup-bucket-name
}

module "auth-server" {
  source                  = "./modules/auth-server"
  sql-instance-name       = module.postgres.instance-name
  sql-connection-name     = module.postgres.connection-name
  image-pull-secret       = kubernetes_secret.dockerhub.metadata[0].name
  inception-key-b64       = local.inception-key-b64
  system-bucket           = google_storage_bucket.system.name
  container-cluster-name  = module.gke-cluster.name
  container-tag           = var.container-tag
}

module "api-gateway" {
  source                 = "./modules/api-gateway"
  image-pull-secret      = kubernetes_secret.dockerhub.metadata[0].name
  archivist_host         = module.archivist.ip-address
  auth_server_host       = module.auth-server.ip-address
  ml_bbq_host            = module.ml-bbq.ip-address
  domain                 = var.zmlp-domain
  container-cluster-name =  module.gke-cluster.name
  container-tag          = var.container-tag
}

module "officer" {
  source                 = "./modules/officer"
  project                = var.project
  zone                   = var.zone
  container-cluster-name = module.gke-cluster.name
  image-pull-secret      = kubernetes_secret.dockerhub.metadata[0].name
  minio-url              = "http://${module.minio.ip-address}:9000"
  minio-access-key       = module.minio.access-key
  minio-secret-key       = module.minio.secret-key
  container-tag          = var.container-tag
}

module "analyst" {
  source                 = "./modules/analyst"
  project                = var.project
  zone                   = var.zone
  container-cluster-name = module.gke-cluster.name
  image-pull-secret      = kubernetes_secret.dockerhub.metadata[0].name
  archivist-url          = "http://${module.archivist.ip-address}"
  officer-url            = "http://${module.officer.ip-address}:7078"
  container-tag          = var.container-tag
}

module "wallet" {
  source                  = "./modules/wallet"
  project                 = var.project
  container-cluster-name  = module.gke-cluster.name
  image-pull-secret       = kubernetes_secret.dockerhub.metadata[0].name
  pg_host                 = module.postgres.ip-address
  sql-instance-name       = module.postgres.instance-name
  sql-service-account-key = module.postgres.sql-service-account-key
  sql-connection-name     = module.postgres.connection-name
  zmlp-api-url            = "http://${module.api-gateway.ip-address}"
  smtp-password           = var.smtp-password
  google-oauth-client-id  = var.google-oauth-client-id
  environment             = var.environment
  inception-key-b64       = local.inception-key-b64
  domain                  = var.wallet-domain
  container-tag           = var.container-tag
  debug                   = var.wallet-debug
  marketplace-project     = "zorroa-public"
  marketplace-credentials = var.marketplace-credentials
}

module "ml-bbq" {
  source                 = "./modules/ml-bbq"
  image-pull-secret      = kubernetes_secret.dockerhub.metadata[0].name
  auth-server-url        = "http://${module.auth-server.ip-address}"
  container-cluster-name = module.gke-cluster.name
  container-tag          = var.container-tag
}

module "gcp-marketplace-integration" {
  source                   = "./modules/gcp-marketplace-integration"
  project                  = var.project
  image-pull-secret        = kubernetes_secret.dockerhub.metadata[0].name
  pg_host                  = module.postgres.ip-address
  sql-instance-name        = module.postgres.instance-name
  sql-service-account-key  = module.postgres.sql-service-account-key
  sql-connection-name      = module.postgres.connection-name
  zmlp-api-url             = "http://${module.api-gateway.ip-address}"
  smtp-password            = var.smtp-password
  google-oauth-client-id   = var.google-oauth-client-id
  marketplace-project      = "zorroa-public"
  marketplace-subscription = "zorroa-public"
  marketplace-credentials  = var.marketplace-credentials
  marketplace-service-name = "zorroa-visual-intelligence-zorroa-public.cloudpartnerservices.goog"
  fqdn                     = var.wallet-domain
  environment              = var.environment
  inception-key-b64        = local.inception-key-b64
  pg_password              = module.wallet.pg_password
  enabled                  = var.deploy-marketplace-integration
  container-tag            = var.container-tag
}
