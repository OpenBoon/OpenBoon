resource "google_pubsub_topic" "swivel" {
  name = "swivel"
}

resource "google_pubsub_subscription" "swivel" {
  name  = "swivel"
  topic = google_pubsub_topic.swivel.name
}

resource "kubernetes_deployment" "swivel" {
  provider = kubernetes
  metadata {
    name = "swivel"
    labels = {
      app = "swivel"
    }
  }
  spec {
    selector {
      match_labels = {
        app = "swivel"
      }
    }
    template {
      metadata {
        labels = {
          app = "swivel"
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
          name  = "swivel"
          image = "boonai/swivel:${var.container-tag}"
          env {
            name  = "GCLOUD_PROJECT"
            value = var.project
          }
          env {
            name  = "SWIVEL_PORT"
            value = "5000"
          }
          env {
            name  = "SWIVEL_SUBSCRIPTION"
            value = google_pubsub_subscription.swivel.name
          }
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 5
            http_get {
              scheme = "HTTP"
              path   = "/health"
              port   = "5000"
            }
          }
          readiness_probe {
            failure_threshold     = 5
            initial_delay_seconds = 1
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/health"
              port   = "5000"
            }
          }
        }
      }
    }
  }
}
