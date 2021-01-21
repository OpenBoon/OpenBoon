
resource "google_storage_bucket" "elasticsearch" {
  lifecycle {
    prevent_destroy = true
  }
  name          = "${var.project}-es-backups"
  storage_class = "MULTI_REGIONAL"
  location      = var.country
  retention_policy {
    retention_period = 86400 * 29
  }
  logging {
    log_bucket = var.log-bucket-name
  }
}

resource "google_service_account" "elasticsearch" {
  project      = var.project
  account_id   = "elasticsearch"
  display_name = "Elasticsearch"
}

resource "google_project_iam_member" "elasticsearch" {
  project = var.project
  role    = "roles/storage.admin"
  member  = "serviceAccount:${google_service_account.elasticsearch.email}"
}

resource "google_service_account_key" "elasticsearch" {
  service_account_id = google_service_account.elasticsearch.name
  keepers = {
    "created_date" : timestamp()
  }
}

resource "kubernetes_secret" "elasticsearch" {
  metadata {
    name      = "elasticsearch-sa-key"
    namespace = var.namespace
  }
  data = {
    "credentials.json" = base64decode(google_service_account_key.elasticsearch.private_key)
  }
  depends_on = [google_service_account_key.elasticsearch]
}

resource "google_container_node_pool" "elasticsearch" {
  name               = var.node-pool-name
  cluster            = var.container-cluster-name
  initial_node_count = 5
  autoscaling {
    max_node_count = 6
    min_node_count = 5
  }
  management {
    auto_repair  = true
    auto_upgrade = true
  }
  node_config {
    machine_type = "custom-6-20480"
    oauth_scopes = [
      "https://www.googleapis.com/auth/compute",
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
    ]
    taint {
      effect = "NO_SCHEDULE"
      key    = "elasticsearch"
      value  = "false"
    }
    labels = {
      type      = "elasticsearch"
      namespace = var.namespace
    }
    disk_type = "pd-ssd"
  }
  lifecycle {
    ignore_changes = [
      initial_node_count,
      autoscaling[0].min_node_count,
      autoscaling[0].max_node_count
    ]
  }
}

resource "kubernetes_storage_class" "elasticsearch" {
  lifecycle {
    prevent_destroy = true
  }
  metadata {
    name = var.storage-class-name
  }
  storage_provisioner = "kubernetes.io/gce-pd"
  parameters = {
    type = "pd-ssd"
  }
}

resource "kubernetes_stateful_set" "elasticsearch-master" {
  provider = kubernetes
  lifecycle {
    prevent_destroy = true
    ignore_changes  = [spec[0].replicas]
  }
  metadata {
    name      = "elasticsearch-master"
    namespace = var.namespace
    labels = {
      app     = "elasticsearch"
      service = "elasticsearch"
    }
  }
  spec {
    update_strategy {
      type = "RollingUpdate"
    }
    service_name = "elasticsearch"
    replicas     = 3
    selector {
      match_labels = {
        app = "elasticsearch"
      }
    }
    volume_claim_template {
      metadata {
        name = "elasticsearch-data"
      }
      spec {
        access_modes       = ["ReadWriteOnce"]
        storage_class_name = kubernetes_storage_class.elasticsearch.metadata[0].name
        resources {
          requests = {
            storage = "100Gi"
          }
        }
      }
    }
    template {
      metadata {
        labels = {
          app = "elasticsearch"
        }
      }
      spec {
        termination_grace_period_seconds = 300
        node_selector = {
          type      = "elasticsearch"
          namespace = var.namespace
        }
        toleration {
          key      = "elasticsearch"
          operator = "Equal"
          value    = "false"
          effect   = "NoSchedule"
        }
        init_container {
          name    = "set-max-map-count"
          image   = "busybox:1.27.2"
          command = ["sysctl", "-w", "vm.max_map_count=262144"]
          security_context {
            privileged                 = true
            allow_privilege_escalation = true
          }
        }
        init_container {
          name    = "set-ulimit"
          image   = "busybox:1.27.2"
          command = ["sh", "-c", "ulimit -n 65536"]
          security_context {
            privileged = true
          }
        }
        init_container {
          name              = "chown-data-dir"
          image_pull_policy = "Always"
          image             = "zmlp/elasticsearch:${var.container-tag}"
          command           = ["chown", "-v", "elasticsearch:elasticsearch", "/usr/share/elasticsearch/data"]
          volume_mount {
            name       = "elasticsearch-data"
            mount_path = "/usr/share/elasticsearch/data"
          }
        }
        image_pull_secrets {
          name = var.image-pull-secret
        }
        volume {
          name = "elasticsearch-sa-key"
          secret {
            secret_name = "elasticsearch-sa-key"
          }
        }
        container {
          name              = "elasticsearch"
          image             = "zmlp/elasticsearch:${var.container-tag}"
          image_pull_policy = "Always"
          env {
            name  = "ES_JAVA_OPTS"
            value = "-Xms512m -Xmx512m"
          }
          env {
            name  = "cluster.name"
            value = "elasticsearch-cluster"
          }
          env {
            name  = "discovery.seed_hosts"
            value = "elasticsearch-master.${var.namespace}.svc.cluster.local"
          }
          env {
            name  = "cluster.initial_master_nodes"
            value = "elasticsearch-master-0,elasticsearch-master-1,elasticsearch-master-2"
          }
          env {
            name  = "node.master"
            value = "true"
          }
          env {
            name  = "node.ingest"
            value = "false"
          }
          env {
            name  = "node.data"
            value = "false"
          }
          env {
            name  = "search.remote.connect"
            value = "false"
          }
          env {
            name = "node.name"
            value_from {
              field_ref {
                field_path = "metadata.name"
              }
            }
          }
          volume_mount {
            name       = "elasticsearch-data"
            mount_path = "/usr/share/elasticsearch/data"
          }
          volume_mount {
            name       = "elasticsearch-sa-key"
            mount_path = "/secrets/gcs/"
            read_only  = true
          }
          resources {
            requests {
              memory = "512Mi"
              cpu    = 1
            }
            limits {
              memory = "1Gi"
              cpu    = 1.5
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_stateful_set" "elasticsearch-data" {
  provider = kubernetes
  lifecycle {
    prevent_destroy = true
    ignore_changes  = [spec[0].replicas]
  }
  metadata {
    name      = "elasticsearch-data"
    namespace = var.namespace
    labels = {
      app     = "elasticsearch"
      service = "elasticsearch"
    }
  }

  spec {
    update_strategy {
      type = "RollingUpdate"
    }
    service_name = "elasticsearch"
    replicas     = 3
    selector {
      match_labels = {
        app = "elasticsearch"
      }
    }
    volume_claim_template {
      metadata {
        name = "elasticsearch-data"
      }
      spec {
        access_modes       = ["ReadWriteOnce"]
        storage_class_name = kubernetes_storage_class.elasticsearch.metadata[0].name
        resources {
          requests = {
            storage = "100Gi"
          }
        }
      }
    }
    template {
      metadata {
        labels = {
          app = "elasticsearch"
        }
      }
      spec {
        termination_grace_period_seconds = 300
        node_selector = {
          type      = "elasticsearch"
          namespace = var.namespace
        }
        toleration {
          key      = "elasticsearch"
          operator = "Equal"
          value    = "false"
          effect   = "NoSchedule"
        }
        init_container {
          name    = "set-max-map-count"
          image   = "busybox:1.27.2"
          command = ["sysctl", "-w", "vm.max_map_count=262144"]
          security_context {
            privileged                 = true
            allow_privilege_escalation = true
          }
        }
        init_container {
          name    = "set-ulimit"
          image   = "busybox:1.27.2"
          command = ["sh", "-c", "ulimit -n 65536"]
          security_context {
            privileged = true
          }
        }
        init_container {
          name              = "chown-data-dir"
          image_pull_policy = "Always"
          image             = "zmlp/elasticsearch:${var.container-tag}"
          command           = ["chown", "-v", "elasticsearch:elasticsearch", "/usr/share/elasticsearch/data"]
          volume_mount {
            name       = "elasticsearch-data"
            mount_path = "/usr/share/elasticsearch/data"
          }
        }
        image_pull_secrets {
          name = var.image-pull-secret
        }
        volume {
          name = "elasticsearch-sa-key"
          secret {
            secret_name = "elasticsearch-sa-key"
          }
        }
        container {
          name              = "elasticsearch"
          image             = "zmlp/elasticsearch:${var.container-tag}"
          image_pull_policy = "Always"
          env {
            name  = "cluster.name"
            value = "elasticsearch-cluster"
          }
          env {
            name = "node.name"
            value_from {
              field_ref {
                field_path = "metadata.name"
              }
            }
          }
          env {
            name  = "discovery.seed_hosts"
            value = "elasticsearch-master.${var.namespace}.svc.cluster.local"
          }
          env {
            name  = "node.master"
            value = "false"
          }
          env {
            name  = "node.ingest"
            value = "true"
          }
          env {
            name  = "node.data"
            value = "true"
          }
          env {
            name  = "cluster.remote.connect"
            value = "true"
          }
          env {
            name  = "ES_JAVA_OPTS"
            value = "-Xms8g -Xmx8g"
          }
          volume_mount {
            name       = "elasticsearch-data"
            mount_path = "/usr/share/elasticsearch/data"
          }
          volume_mount {
            name       = "elasticsearch-sa-key"
            mount_path = "/secrets/gcs/"
            read_only  = true
          }
          resources {
            requests {
              memory = "15Gi"
              cpu    = 4
            }
            limits {
              memory = "16Gi"
              cpu    = 4.1
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "elasticsearch-master" {
  metadata {
    name      = "elasticsearch-master"
    namespace = var.namespace
    labels = {
      app = "elasticsearch"
    }
  }
  spec {
    cluster_ip = var.ip-address
    port {
      name     = "9200-to-9200-tcp"
      protocol = "TCP"
      port     = 9200
    }
    port {
      name     = "9300-to-9300-tcp"
      protocol = "TCP"
      port     = 9300
    }
    selector = {
      app = "elasticsearch"
    }
    type = "ClusterIP"
  }
}

resource "kubernetes_service" "elasticsearch-data" {
  metadata {
    name      = "elasticsearch-data"
    namespace = var.namespace
    labels = {
      app = "elasticsearch"
    }
  }
  spec {
    cluster_ip = "None"
    port {
      name     = "9200-to-9200-tcp"
      protocol = "TCP"
      port     = 9200
    }
    port {
      name     = "9300-to-9300-tcp"
      protocol = "TCP"
      port     = 9300
    }
    selector = {
      app = "elasticsearch"
    }
    type = "ClusterIP"
  }
}

