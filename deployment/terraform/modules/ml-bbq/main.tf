resource "kubernetes_deployment" "ml-bbq" {
  provider = kubernetes
  lifecycle {
    ignore_changes = [spec[0].replicas]
  }
  metadata {
    name      = "ml-bbq"
    namespace = var.namespace
    labels = {
      app = "ml-bbq"
    }
  }
  spec {
    replicas = 2
    selector {
      match_labels = {
        app = "ml-bbq"
      }
    }
    strategy {
      type = var.rollout-strategy
    }
    template {
      metadata {
        labels = {
          app = "ml-bbq"
        }
      }
      spec {
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
          name              = "ml-bbq"
          image             = "boonai/ml-bbq:${var.container-tag}"
          image_pull_policy = "Always"
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
            name  = "ZMLP_SECURITY_AUTHSERVER_URL"
            value = var.auth-server-url
          }
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 5
            http_get {
              scheme = "HTTP"
              path   = "/healthcheck"
              port   = "8282"
            }
          }
          readiness_probe {
            failure_threshold     = 5
            initial_delay_seconds = 1
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/healthcheck"
              port   = "8282"
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

resource "kubernetes_horizontal_pod_autoscaler" "ml-bbq" {
  provider = kubernetes
  metadata {
    name      = "ml-bbq-hpa"
    namespace = var.namespace
    labels = {
      app = "ml-bbq"
    }
  }
  spec {
    max_replicas = 2
    min_replicas = 1
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = "ml-bbq"
    }
    target_cpu_utilization_percentage = 75
  }
  lifecycle {
    ignore_changes = [spec[0].max_replicas, spec[0].min_replicas]
  }
}

resource "kubernetes_service" "ml-bbq" {
  metadata {
    name      = "ml-bbq-service"
    namespace = var.namespace
    labels = {
      app = "ml-bbq"
    }
  }
  spec {
    cluster_ip = var.ip-address
    port {
      name     = "8282-to-8282-tcp"
      protocol = "TCP"
      port     = "8282"
    }
    selector = {
      app = "ml-bbq"
    }
    type = "ClusterIP"
  }
}

