resource "kubernetes_deployment" "reporter" {
  metadata {
    name      = "reporter"
    namespace = var.namespace
    labels = {
      app = "reporter"
    }
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "reporter"
      }
    }
    template {
      metadata {
        labels = {
          app = "reporter"
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
          name              = "reporter"
          image             = "zmlp/reporter:${var.container-tag}"
          image_pull_policy = "Always"
          env {
            name  = "PROJECT_ID"
            value = var.project
          }
          env {
            name  = "ZMLP_API_URL"
            value = var.zmlp-api-url
          }
          env {
            name  = "INCEPTION_KEY_B64"
            value = var.inception-key-b64
          }
          env {
            name  = "COLLECTION_INTERVAL"
            value = var.collection-interval
          }
        }
      }
    }
  }
}
