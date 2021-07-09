
resource "google_pubsub_subscription" "tugboat-model-events" {
  name  = "tugboat-model-events"
  topic = "model-events"
}

resource "kubernetes_deployment" "tugboat" {
  provider = kubernetes
  metadata {
    name = "tugboat"
    labels = {
      app = "tugboat"
    }
  }
  spec {
    selector {
      match_labels = {
        app = "tugboat"
      }
    }
    template {
      metadata {
        labels = {
          app = "tugboat"
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
          name  = "tugboat"
          image = "boonai/tugboat:${var.container-tag}"
          env {
            name  = "GCLOUD_PROJECT"
            value = var.project
          }
          env {
            name  = "PORT"
            value = "9393"
          }
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 5
            http_get {
              scheme = "HTTP"
              path   = "/health"
              port   = "9393"
            }
          }
          readiness_probe {
            failure_threshold     = 5
            initial_delay_seconds = 1
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/health"
              port   = "9393"
            }
          }
        }
      }
    }
  }
}
