resource "google_project_service" "iam" {
  service            = "iam.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "service-usage" {
  service            = "serviceusage.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "sqladmin" {
  service            = "sqladmin.googleapis.com"
  disable_on_destroy = false
  depends_on         = [google_project_service.service-usage]
}

resource "google_sql_database_instance" "zmlp" {
  lifecycle {
    prevent_destroy = true
  }
  name             = "zmlp"
  database_version = "POSTGRES_9_6"
  region           = var.region
  settings {
    tier = var.sql-tier
    ip_configuration {
      ipv4_enabled = true
      require_ssl  = true
    }
    backup_configuration {
      enabled = true
    }
  }
  depends_on = [google_project_service.sqladmin]
}

resource "google_service_account" "cloud-sql-proxy" {
  project      = var.project
  account_id   = "zmlp-cloud-sql-proxy"
  display_name = "CLoud SQL Proxy"
}

resource "google_project_iam_member" "cloud-sql-proxy-iam" {
  project = var.project
  role    = "roles/cloudsql.admin"
  member  = "serviceAccount:${google_service_account.cloud-sql-proxy.email}"
}

resource "google_service_account_key" "cloud-sql-account-key" {
  service_account_id = google_service_account.cloud-sql-proxy.name
  keepers = {
    "created_date" : timestamp()
  }
}

resource "kubernetes_secret" "cloud-sql-sa-key" {
  metadata {
    name      = "cloud-sql-sa-key"
    namespace = var.namespace
  }
  data = {
    "credentials.json" = base64decode(google_service_account_key.cloud-sql-account-key.private_key)
  }
}
