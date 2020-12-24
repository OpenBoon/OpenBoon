resource "google_project_service" "monitoring" {
  service            = "monitoring.googleapis.com"
  disable_on_destroy = false
  depends_on         = [google_project_service.service-usage]
}

resource "google_project_service" "container" {
  service            = "container.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "logging" {
  service            = "logging.googleapis.com"
  disable_on_destroy = false
  depends_on         = [google_project_service.service-usage]
}

resource "google_project_service" "cloudresourcemanager" {
  service            = "cloudresourcemanager.googleapis.com"
  disable_on_destroy = false
  depends_on         = [google_project_service.service-usage]
}

resource "random_password" "cluster-password" {
  length  = 16
  special = true
}

resource "google_project_service" "service-usage" {
  service            = "serviceusage.googleapis.com"
  disable_on_destroy = false
}

resource "google_container_cluster" "primary" {
  provider = google-beta
  lifecycle {
    prevent_destroy = true
  }

  name               = "zmlp"
  location           = var.zone
  cluster_ipv4_cidr  = "10.0.0.0/14"
  initial_node_count = var.initial-node-count
  monitoring_service = "monitoring.googleapis.com/kubernetes"
  logging_service    = "logging.googleapis.com/kubernetes"
  release_channel {
    channel = "STABLE"
  }

  master_auth {
    username = "zorroa"
    password = random_password.cluster-password.result
  }

  depends_on = [
    google_project_service.logging,
    google_project_service.monitoring,
    google_project_service.container,
    google_project_service.cloudresourcemanager,
    google_project_service.service-usage,
  ]
}

resource "google_container_node_pool" "default" {
  name               = "default"
  cluster            = google_container_cluster.primary.name
  initial_node_count = 1
  autoscaling {
    max_node_count = "10"
    min_node_count = "1"
  }
  management {
    auto_repair  = true
    auto_upgrade = true
  }
  node_config {
    machine_type = "n1-standard-4"
    oauth_scopes = [
      "compute-rw",
      "storage-rw",
      "logging-write",
      "https://www.googleapis.com/auth/pubsub",
      "https://www.googleapis.com/auth/devstorage.full_control",
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/sqlservice.admin",
    ]
    labels = {
      type = "default"
    }
  }
  depends_on = [google_container_cluster.primary]
  lifecycle {
    ignore_changes = [
      initial_node_count,
      autoscaling[0].min_node_count,
      autoscaling[0].max_node_count
    ]
  }
}

