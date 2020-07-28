resource "kubernetes_deployment" "elasticsearch-hq" {
  provider = kubernetes
  metadata {
    name      = "elasticsearch-hq"
    namespace = var.namespace
    labels = {
      app = "elasticsearch-hq"
    }
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "elasticsearch-hq"
      }
    }
    template {
      metadata {
        labels = {
          app = "elasticsearch-hq"
        }
      }
      spec {
        node_selector = {
          type      = "default"
          namespace = var.namespace
        }
        container {
          name              = "elasticsearch-hq"
          image             = "elastichq/elasticsearch-hq:latest"
          image_pull_policy = "Always"
          env {
            name  = "HQ_DEFAULT_URL"
            value = "http://${var.es-ip-address}:9200"
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
