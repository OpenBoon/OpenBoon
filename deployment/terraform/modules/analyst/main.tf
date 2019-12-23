
resource "google_container_node_pool" "analyst" {
  name = "${var.node-pool-name}"
  cluster = "${var.container-cluster-name}"
  initial_node_count = 1
  autoscaling {
    max_node_count = "${var.maximum-nodes}"
    min_node_count = "${var.minimum-nodes}"
  }
  management {
    auto_repair = true
    auto_upgrade = true
  }
  node_config {
    machine_type = "${var.machine-type}"
    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/cloud-platform",
      "https://www.googleapis.com/auth/devstorage.read_only"
    ]
    taint {
      effect = "NO_SCHEDULE"
      key = "analyst"
      value = "false"
    }
    labels {
      type = "analyst"
      namespace = "${var.namespace}"
    }
  }
}

resource "kubernetes_deployment" "analyst" {
  provider = "kubernetes"
  metadata {
    name = "analyst"
    namespace = "${var.namespace}"
    labels {
      app = "analyst"
    }
  }
  spec {
    selector {
      match_labels {
        app = "analyst"
      }
    }
    strategy {
      type = "${var.rollout-strategy}"
    }
    template {
      metadata {
        labels {
          app = "analyst"
        }
      }
      spec {
        node_selector {
          type = "analyst"
          namespace = "${var.namespace}"
        }
        image_pull_secrets {
          name = "${var.image-pull-secret}"
        }
        toleration {
          key = "analyst"
          operator = "Equal"
          value = "false"
          effect = "NoSchedule"
        }
        container {
          name = "analyst"
          image = "zmlp/analyst:${var.container-tag}"
          image_pull_policy = "Always"
          env {
            name = "PIXML_SERVER"
            value = "${var.archivist-url}"
          }
          env {
            name = "ZORROA_ARCHIVIST_URL"
            value = "${var.archivist-url}"
          }
          liveness_probe = {
            initial_delay_seconds = 120
            period_seconds = 5
            http_get {
              scheme = "HTTPS"
              path = "/"
              port = "5000"
            }
          }
          readiness_probe = {
            failure_threshold = 5
            initial_delay_seconds = 1
            period_seconds = 30
            http_get {
              scheme = "HTTPS"
              path = "/"
              port = "5000"
            }
          }
          resources {
            requests {
              memory = "${var.memory-request}"
              cpu = "${var.cpu-request}"
            }
            limits {
              memory = "${var.memory-limit}"
              cpu = "${var.cpu-limit}"
            }
          }
        }
      }
    }
  }
}


resource "kubernetes_horizontal_pod_autoscaler" "analyst" {
  provider = "kubernetes"
  metadata {
    name = "analyst-hpa"
    namespace = "${var.namespace}"
    labels {
      app = "analyst"
    }
  }
  spec {
    max_replicas = "${var.maximum-replicas}"
    min_replicas = "${var.minimum-replicas}"
    scale_target_ref {
      api_version = "apps/v1"
      kind = "Deployment"
      name = "analyst"
    }
    target_cpu_utilization_percentage = 75
  }
}


resource "kubernetes_service" "analyst" {
  metadata {
    name = "analyst-service"
    namespace = "${var.namespace}"
    labels {
      app = "analyst"
    }
  }
  spec {
    cluster_ip = "${var.ip-address}"
    port {
      name = "7078-to-7078-tcp"
      protocol = "TCP"
      port = 7078
    }
    selector {
      app = "analyst"
    }
    type = "ClusterIP"
  }
}
