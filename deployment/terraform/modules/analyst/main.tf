resource "google_container_node_pool" "analyst" {
  name               = var.node-pool-name
  cluster            = var.container-cluster-name
  initial_node_count = 1
  autoscaling {
    max_node_count = 3
    min_node_count = 1
  }
  management {
    auto_repair  = true
    auto_upgrade = true
  }

  node_config {
    preemptible  = true
    machine_type = var.machine-type
    disk_size_gb = 500
    disk_type    = "pd-ssd"
    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/cloud-platform",
      "https://www.googleapis.com/auth/devstorage.read_only",
    ]
    taint {
      effect = "NO_SCHEDULE"
      key    = "analyst"
      value  = "false"
    }
    labels = {
      type = "analyst"
    }
  }
  lifecycle {
    ignore_changes = [
      initial_node_count,
      autoscaling[0].min_node_count,
      autoscaling[0].max_node_count
    ]
  }
}

resource "random_string" "analyst-shared-key" {
  length  = 50
  special = false
}

resource "kubernetes_deployment" "analyst" {
  provider = kubernetes
  lifecycle {
    ignore_changes = [spec[0].replicas]
  }
  metadata {
    name      = "analyst"
    namespace = var.namespace
    labels = {
      app = "analyst"
    }
  }
  spec {
    replicas = 2
    selector {
      match_labels = {
        app = "analyst"
      }
    }
    strategy {
      type = var.rollout-strategy
    }
    template {
      metadata {
        labels = {
          app = "analyst"
        }
      }
      spec {
        termination_grace_period_seconds = 10800
        node_selector = {
          type = "analyst"
        }
        image_pull_secrets {
          name = var.image-pull-secret
        }
        toleration {
          key      = "analyst"
          operator = "Equal"
          value    = "false"
          effect   = "NoSchedule"
        }
        volume {
          name = "tmp"
          host_path {
            path = "/mnt/stateful_partition/var/tmp"
          }
        }
        volume {
          name = "dockersock"
          host_path {
            path = "/var/run/docker.sock"
          }
        }
        volume {
          name = "dockerhubcreds"
          secret {
            secret_name = "dockerhubcreds"
          }
        }
        container {
          name              = "analyst"
          image             = "boonai/analyst:${var.container-tag}"
          image_pull_policy = "Always"
          volume_mount {
            mount_path = "/tmp"
            name       = "tmp"
          }
          volume_mount {
            mount_path = "/var/run/docker.sock"
            name       = "dockersock"
          }
          volume_mount {
            name       = "dockerhubcreds"
            mount_path = "/etc/docker"
            read_only  = true
          }
          env {
            name  = "BOONAI_SERVER"
            value = var.archivist-url
          }
          env {
            name  = "ANALYST_SHAREDKEY"
            value = random_string.analyst-shared-key.result
          }
          env {
            name  = "OFFICER_URL"
            value = var.officer-url
          }
          env {
            name  = "ANALYST_TEMP"
            value = "/mnt/stateful_partition/var/tmp"
          }
          env {
            name  = "CLUSTER_TAG"
            value = var.container-tag
          }
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 5
            http_get {
              scheme = "HTTP"
              path   = "/"
              port   = "5000"
            }
          }
          readiness_probe {
            failure_threshold     = 5
            initial_delay_seconds = 1
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/"
              port   = "5000"
            }
          }
          resources {
            requests = {
              memory = var.memory-request
              cpu    = var.cpu-request
            }
            limits = {
              memory = var.memory-limit
              cpu    = var.cpu-limit
            }
          }
          lifecycle {
            pre_stop {
              exec {
                command = ["/service/bin/k8prestop.py"]
              }
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_horizontal_pod_autoscaler" "analyst" {
  provider = kubernetes
  metadata {
    name      = "analyst-hpa"
    namespace = var.namespace
    labels = {
      app = "analyst"
    }
  }
  spec {
    max_replicas = 20
    min_replicas = 5
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = "analyst"
    }
    metric {
      type = "External"
      external {
        metric {
          name = "custom.googleapis.com|boon|analyst-scale-ratio"
          selector {}
        }
        target {
          type  = "Value"
          value = 1
        }
      }
    }
  }
  lifecycle {
    ignore_changes = [spec[0].max_replicas, spec[0].min_replicas]
  }
}

resource "kubernetes_service" "analyst" {
  metadata {
    name      = "analyst-service"
    namespace = var.namespace
    labels = {
      app = "analyst"
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
      app = "analyst"
    }
    type = "ClusterIP"
  }
}

