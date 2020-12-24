resource "kubernetes_persistent_volume_claim" "minio" {
  lifecycle {
    prevent_destroy = true
  }
  metadata {
    name      = "minio-pv-claim"
    namespace = var.namespace
  }
  spec {
    access_modes = ["ReadWriteOnce"]
    resources {
      requests = {
        storage = "100Gi"
      }
    }
  }
}

resource "random_string" "access_key" {
  length  = 16
  special = false
}

resource "random_password" "secret_key" {
  length  = 16
  special = false
}

resource "kubernetes_deployment" "minio" {
  metadata {
    name      = "minio"
    namespace = var.namespace
    labels = {
      app = "minio"
    }
  }
  spec {
    selector {
      match_labels = {
        app = "minio"
      }
    }
    strategy {
      type = "Recreate"
    }
    template {
      metadata {
        labels = {
          app = "minio"
        }
      }
      spec {
        volume {
          name = "data"
          persistent_volume_claim {
            claim_name = "minio-pv-claim"
          }
        }
        container {
          name = "minio"
          volume_mount {
            mount_path = "/data"
            name       = "data"
          }
          image = "minio/minio:RELEASE.2019-10-12T01-39-57Z"
          args  = ["server", "/data"]
          env {
            name  = "MINIO_ACCESS_KEY"
            value = random_string.access_key.result
          }
          env {
            name  = "MINIO_SECRET_KEY"
            value = random_password.secret_key.result
          }
          port {
            container_port = 9000
          }
          readiness_probe {
            http_get {
              path = "/minio/health/ready"
              port = 9000
            }
            initial_delay_seconds = 30
            failure_threshold     = 4
            period_seconds        = 20
          }
          liveness_probe {
            http_get {
              path = "/minio/health/live"
              port = 9000
            }
            initial_delay_seconds = 120
            period_seconds        = 20
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "minio" {
  metadata {
    name      = "minio"
    namespace = var.namespace
    labels = {
      app = "minio"
    }
  }
  spec {
    cluster_ip = var.ip-address
    port {
      name     = "9000-to-9000-tcp"
      protocol = "TCP"
      port     = 9000
    }
    selector = {
      app = "minio"
    }
    type = "ClusterIP"
  }
}

