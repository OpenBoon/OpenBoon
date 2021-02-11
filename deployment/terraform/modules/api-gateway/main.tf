resource "google_compute_global_address" "api-gateway-external" {
  name = var.external-ip-name
}

resource "kubernetes_deployment" "api-gateway" {
  provider = kubernetes
  lifecycle {
    ignore_changes = [spec[0].replicas]
  }
  metadata {
    name      = "api-gateway"
    namespace = var.namespace
    labels = {
      app = "api-gateway"
    }
  }
  spec {
    replicas = 2
    selector {
      match_labels = {
        app = "api-gateway"
      }
    }
    template {
      metadata {
        labels = {
          app = "api-gateway"
        }
      }
      spec {
        node_selector = {
          type = "default"
        }
        image_pull_secrets {
          name = var.image-pull-secret
        }
        container {
          name              = "api-gateway"
          image             = "zmlp/apigateway:${var.container-tag}"
          image_pull_policy = "Always"
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 5
            http_get {
              scheme = "HTTP"
              path   = "/monitor/health"
              port   = "80"
            }
            timeout_seconds = 5
          }
          readiness_probe {
            failure_threshold     = 6
            initial_delay_seconds = 30
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/monitor/health"
              port   = "80"
            }
          }
          port {
            container_port = "80"
          }
          resources {
            limits {
              memory = "2Gi"
              cpu    = 2
            }
            requests {
              memory = "512Mi"
              cpu    = 1
            }
          }
          env {
            name  = "ARCHIVIST_HOST"
            value = var.archivist_host
          }
          env {
            name  = "AUTH_SERVER_HOST"
            value = var.auth_server_host
          }
          env {
            name  = "MLBBQ_SERVER_HOST"
            value = var.ml_bbq_host
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "api-gateway" {
  metadata {
    name      = "api-gateway-service"
    namespace = var.namespace
    labels = {
      app = "api-gateway"
    }
  }
  spec {
    port {
      name     = "http"
      protocol = "TCP"
      port     = 80
    }
    selector = {
      app = "api-gateway"
    }
    type = "NodePort"
  }
}

resource "google_compute_managed_ssl_certificate" "default" {
  provider = google-beta
  name     = "api-gateway-cert"
  managed {
    domains = [var.domains]
  }
}

resource "kubernetes_ingress" "api-gateway" {
  metadata {
    name      = "api-gateway-ingress"
    namespace = var.namespace
    annotations = {
      "kubernetes.io/ingress.allow-http"            = "false"
      "ingress.gcp.kubernetes.io/pre-shared-cert"   = google_compute_managed_ssl_certificate.default.name
      "kubernetes.io/ingress.global-static-ip-name" = google_compute_global_address.api-gateway-external.name
    }
  }
  spec {
    backend {
      service_name = "api-gateway-service"
      service_port = 80
    }
  }
}

resource "kubernetes_horizontal_pod_autoscaler" "api-gateway" {
  provider = kubernetes
  metadata {
    name      = "api-gateway-hpa"
    namespace = var.namespace
    labels = {
      app = "api-gateway"
    }
  }
  spec {
    max_replicas = 10
    min_replicas = 2
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = "api-gateway"
    }
    target_cpu_utilization_percentage = 80
  }
  lifecycle {
    ignore_changes = [spec[0].max_replicas, spec[0].min_replicas]
  }
}

