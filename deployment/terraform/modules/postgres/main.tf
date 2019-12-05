resource "google_sql_database_instance" "zmlp" {
  name = "zmlp"
  database_version = "POSTGRES_9_6"
  region = "${var.region}"
  settings {
    tier = "${var.sql-tier}"
    ip_configuration {
      ipv4_enabled = true
    }
    backup_configuration {
      enabled = true
    }
  }
}

resource "google_project_service" "iam" {
  service = "iam.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "sqladmin" {
  service = "sqladmin.googleapis.com"
  disable_on_destroy = false
}

resource "google_service_account" "cloud-sql-proxy" {
  project = "${var.project}"
  account_id = "zmlp-cloud-sql-proxy"
  display_name = "CLoud SQL Proxy"
}

resource "google_project_iam_member" "cloud-sql-proxy-iam" {
  project = "${var.project}"
  role = "roles/editor"
  member = "serviceAccount:${google_service_account.cloud-sql-proxy.email}"
}

resource "google_service_account_key" "cloud-sql-account-key" {
  service_account_id = "${google_service_account.cloud-sql-proxy.name}"
}
