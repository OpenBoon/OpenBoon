## Store state in GCS. ###################################################################
terraform {
  backend "remote" {
    hostname     = "app.terraform.io"
    organization = "zorroa"

    workspaces {
      name = "01-Development"
    }
  }
}

## Providers #############################################################################
provider "google" {
  credentials = var.terraform-credentials
  project     = var.project
  region      = "${var.country}-${var.region}"
  zone        = "${var.country}-${var.region}-${var.zone}"
}

provider "google-beta" {
  credentials = var.terraform-credentials
  project     = var.project
  region      = "${var.country}-${var.region}"
  zone        = "${var.country}-${var.region}-${var.zone}"
}

provider "azurerm" {
  subscription_id = var.azure-subscription-id
  client_id       = var.azure-client-id
  tenant_id       = var.azure-tenant-id
  client_secret   = var.azure-client-secret
  features {}
}

provider "aws" {
  region     = var.aws-region
  access_key = var.aws-key
  secret_key = var.aws-secret
}

provider "kubernetes" {
  load_config_file       = "false"
  host                   = module.gke-cluster.endpoint
  username               = module.gke-cluster.username
  password               = module.gke-cluster.password
  client_certificate     = module.gke-cluster.client_certificate
  client_key             = module.gke-cluster.client_key
  cluster_ca_certificate = module.gke-cluster.cluster_ca_certificate
}

## GCP Infrastructure ###################################################################
module "gke-cluster" {
  source = "./modules/gke-cluster"
  zone   = local.zone
}

module "stackdriver-adapter" {
  source = "./modules/stackdriver-adapter"
}

module "postgres" {
  source   = "./modules/postgres"
  project  = var.project
  region   = local.region
  sql-tier = var.sql-tier
}

module "redis" {
  source = "./modules/redis"
}

module "minio" {
  source = "./modules/minio"
}

resource "google_storage_bucket" "access-logs" {
  lifecycle {
    prevent_destroy = true
  }
  name = "${var.project}-zmlp-bucket-access-logs"
  versioning {
    enabled = true
  }
}

resource "google_storage_bucket" "system" {
  lifecycle {
    prevent_destroy = true
  }
  name = "${var.project}-zmlp-system-bucket"
  versioning {
    enabled = true
  }
  logging {
    log_bucket = google_storage_bucket.access-logs.name
  }
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

resource "google_storage_bucket_object" "task_env" {
  bucket  = google_storage_bucket.system.name
  name    = "environments/task_env.json"
  content = <<EOF
{
  "ENVIRONMENT": "${var.environment}",
  "CLARIFAI_KEY":  "${var.clarifai-key}",
  "ZORROA_AWS_KEY": "${var.aws-key}",
  "ZORROA_AWS_SECRET": "${var.aws-secret}",
  "ZORROA_AWS_REGION": "${var.aws-region}",
  "ZORROA_AWS_BUCKET": "${module.aws-ml.bucket}",
  "ZORROA_AWS_ML_USER_ROLE_ARN": "${module.aws-ml.ml-user-role-arn}",
  "ZORROA_AWS_ML_USER_SQS_URL": "${module.aws-ml.ml-user-sqs-url}",
  "ZORROA_AWS_ML_USER_SQS_ARN": "${module.aws-ml.ml-user-sqs-arn}",
  "ZORROA_AWS_ML_USER_SNS_TOPIC_ARN": "${module.aws-ml.ml-user-sns-topic-arn}",
  "ZORROA_AZURE_VISION_REGION": "${module.azure-ml.vision-region}",
  "ZORROA_AZURE_VISION_ENDPOINT": "${module.azure-ml.vision-endpoint}",
  "ZORROA_AZURE_VISION_KEY": "${module.azure-ml.vision-key}",
  "ZMLP_BILLING_METRICS_SERVICE": "http://${module.metrics.ip-address}"
}
EOF

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

resource "google_project_service" "dlp" {
  service            = "dlp.googleapis.com"
  disable_on_destroy = false
}

## Third Party ML Resources #################################################################
module "azure-ml" {
  source      = "./modules/azure-ml"
  environment = var.environment
}

module "aws-ml" {
  source      = "./modules/aws-ml"
  environment = var.environment
}

## ZMLP Services ######################################################################
module "elasticsearch" {
  source                 = "./modules/elasticsearch"
  project                = var.project
  country                = var.country
  container-cluster-name = module.gke-cluster.name
  image-pull-secret      = kubernetes_secret.dockerhub.metadata[0].name
  container-tag          = var.container-tag
  log-bucket-name        = google_storage_bucket.access-logs.name
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
  system-bucket           = google_storage_bucket.system.name
  container-cluster-name  = module.gke-cluster.name
  analyst-shared-key      = module.analyst.shared-key
  container-tag           = var.container-tag
  es-backup-bucket-name   = module.elasticsearch.backup-bucket-name
  log-bucket-name         = google_storage_bucket.access-logs.name
}

module "auth-server" {
  source                 = "./modules/auth-server"
  sql-instance-name      = module.postgres.instance-name
  sql-connection-name    = module.postgres.connection-name
  image-pull-secret      = kubernetes_secret.dockerhub.metadata[0].name
  inception-key-b64      = local.inception-key-b64
  system-bucket          = google_storage_bucket.system.name
  container-cluster-name = module.gke-cluster.name
  container-tag          = var.container-tag
}

module "api-gateway" {
  source                 = "./modules/api-gateway"
  image-pull-secret      = kubernetes_secret.dockerhub.metadata[0].name
  archivist_host         = module.archivist.ip-address
  auth_server_host       = module.auth-server.ip-address
  ml_bbq_host            = "${module.ml-bbq.ip-address}:8282"
  domain                 = var.zmlp-domain
  container-cluster-name = module.gke-cluster.name
  container-tag          = var.container-tag
}

module "officer" {
  source                 = "./modules/officer"
  project                = var.project
  zone                   = var.zone
  container-cluster-name = module.gke-cluster.name
  image-pull-secret      = kubernetes_secret.dockerhub.metadata[0].name
  container-tag          = var.container-tag
  redis-host             = "${module.redis.ip-address}:6379"
  data-bucket-name       = module.archivist.data-bucket-name
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
  memory-request         = var.analyst-memory-request
  memory-limit           = var.analyst-memory-limit
  cpu-request            = var.analyst-cpu-request
  cpu-limit              = var.analyst-cpu-limit
  machine-type           = var.analyst-machine-type
}

module "wallet" {
  source                          = "./modules/wallet"
  project                         = var.project
  container-cluster-name          = module.gke-cluster.name
  image-pull-secret               = kubernetes_secret.dockerhub.metadata[0].name
  pg_host                         = module.postgres.ip-address
  sql-instance-name               = module.postgres.instance-name
  sql-service-account-key         = module.postgres.sql-service-account-key
  sql-connection-name             = module.postgres.connection-name
  zmlp-api-url                    = "http://${module.api-gateway.ip-address}"
  smtp-password                   = var.smtp-password
  google-oauth-client-id          = var.google-oauth-client-id
  environment                     = var.environment
  inception-key-b64               = local.inception-key-b64
  domain                          = var.wallet-domain
  container-tag                   = var.container-tag
  browsable                       = var.wallet-browsable-api
  marketplace-project             = "zorroa-public"
  marketplace-credentials         = var.marketplace-credentials
  superadmin                      = var.wallet-superadmin
  use-model-ids-for-label-filters = var.wallet-use-model-ids-for-label-filters
  metrics-ip-address              = module.metrics.ip-address
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

module "elasticsearch-hq" {
  source = "./modules/elasticsearch-hq"
}

module "reporter" {
  source            = "./modules/reporter"
  inception-key-b64 = local.inception-key-b64
  project           = var.project
  container-tag     = var.container-tag
  image-pull-secret = kubernetes_secret.dockerhub.metadata[0].name
  zmlp-api-url      = "http://${module.api-gateway.ip-address}"
  monitor-password  = module.archivist.monitor-password
}

module "metrics" {
  source               = "./modules/metrics"
  sql-instance-name    = module.postgres.instance-name
  sql-connection-name  = module.postgres.connection-name
  image-pull-secret    = kubernetes_secret.dockerhub.metadata[0].name
  environment          = var.environment
  container-tag        = var.container-tag
  browsable            = var.metrics-browsable
  debug                = var.metrics-debug
  superuser-email      = var.metrics-superuser-email
  superuser-password   = var.metrics-superuser-password
  superuser-first-name = var.metrics-superuser-first-name
  superuser-last-name  = var.metrics-superuser-last-name
  django-log-level     = var.metrics-django-log-level
  log-requests         = var.metrics-log-requests
}
