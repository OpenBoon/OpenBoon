resource "google_container_node_pool" "officer" {
  name               = var.node-pool-name
  cluster            = var.container-cluster-name
  initial_node_count = 1
  autoscaling {
    max_node_count = var.maximum-nodes
    min_node_count = var.minimum-nodes
  }
  management {
    auto_repair  = true
    auto_upgrade = true
  }
  node_config {
    machine_type = var.machine-type
    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/cloud-platform",
      "https://www.googleapis.com/auth/devstorage.read_only",
    ]
    taint {
      effect = "NO_SCHEDULE"
      key    = "officer"
      value  = "false"
    }
    labels = {
      type      = "officer"
      namespace = var.namespace
    }
  }
  lifecycle {
    ignore_changes = [
      autoscaling[0].min_node_count,
      autoscaling[0].max_node_count
    ]
  }
}

resource "kubernetes_deployment" "officer" {
  provider = kubernetes
  lifecycle {
    ignore_changes = [spec[0].replicas]
  }
  metadata {
    name      = "officer"
    namespace = var.namespace
    labels = {
      app = "officer"
    }
  }
  spec {
    replicas = 2
    selector {
      match_labels = {
        app = "officer"
      }
    }
    strategy {
      type = var.rollout-strategy
    }
    template {
      metadata {
        labels = {
          app = "officer"
        }
      }
      spec {
        node_selector = {
          type      = "officer"
          namespace = var.namespace
        }
        image_pull_secrets {
          name = var.image-pull-secret
        }
        toleration {
          key      = "officer"
          operator = "Equal"
          value    = "false"
          effect   = "NoSchedule"
        }
        container {
          name              = "officer"
          image             = "zmlp/officer:${var.container-tag}"
          image_pull_policy = "Always"
          env {
            name  = "ZMLP_STORAGE_PROJECT_BUCKET"
            value = var.data-bucket-name
          }
          env {
            name  = "REDIS_HOST"
            value = var.redis-host
          }
          env {
            name = "ZMLP_STORAGE_CLIENT"
            value = "gcs"
          }
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 5
            http_get {
              scheme = "HTTP"
              path   = "/monitor/health"
              port   = "7078"
            }
          }
          readiness_probe {
            failure_threshold     = 5
            initial_delay_seconds = 1
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/monitor/health"
              port   = "7078"
            }
          }
          resources {
            requests {
              memory = var.memory-request
              cpu    = var.cpu-request
            }
            limits {
              memory = var.memory-limit
              cpu    = var.cpu-limit
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_horizontal_pod_autoscaler" "officer" {
  provider = kubernetes
  metadata {
    name      = "officer-hpa"
    namespace = var.namespace
    labels = {
      app = "officer"
    }
  }
  spec {
    max_replicas = var.maximum-replicas
    min_replicas = var.minimum-replicas
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = "officer"
    }
    target_cpu_utilization_percentage = 75
  }
  lifecycle {
    ignore_changes = [spec[0].max_replicas, spec[0].min_replicas]
  }
}

resource "kubernetes_service" "officer" {
  metadata {
    name      = "officer-service"
    namespace = var.namespace
    labels = {
      app = "officer"
    }
  }
  spec {
    cluster_ip = var.ip-address
    port {
      name     = "7078-to-7078-tcp"
      protocol = "TCP"
      port     = 7078
    }
    selector = {
      app = "officer"
    }
    type = "ClusterIP"
  }
}

