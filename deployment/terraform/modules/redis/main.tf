resource "kubernetes_storage_class" "redis" {
  lifecycle {
    prevent_destroy = true
  }
  metadata {
    name = var.storage-class-name
  }
  storage_provisioner = "kubernetes.io/gce-pd"
  parameters = {
    type = "pd-ssd"
  }
}

resource "kubernetes_stateful_set" "redis" {
  provider = kubernetes
  lifecycle {
    prevent_destroy = true
    ignore_changes  = [spec[0].replicas]
  }
  metadata {
    name      = "redis"
    namespace = var.namespace
    labels = {
      app     = "redis"
      service = "redis"
    }
  }

  spec {
    update_strategy {
      type = "RollingUpdate"
    }
    service_name = "redis"
    replicas     = 1
    selector {
      match_labels = {
        app = "redis"
      }
    }
    volume_claim_template {
      metadata {
        name = "redis-data"
      }
      spec {
        access_modes       = ["ReadWriteOnce"]
        storage_class_name = kubernetes_storage_class.redis.metadata[0].name
        resources {
          requests = {
            storage = "50Gi"
          }
        }
      }
    }
    template {
      metadata {
        labels = {
          app = "redis"
        }
      }
      spec {
        termination_grace_period_seconds = 300
        node_selector = {
          type = "default"
        }
        init_container {
          name              = "chown-data-dir"
          image_pull_policy = "Always"
          image             = "redis:${var.container-tag}"
          command           = ["chown", "-v", "redis:redis", "/data"]
          volume_mount {
            name       = "redis-data"
            mount_path = "/usr/share/redis/data"
          }
        }
        container {
          name              = "redis"
          image             = "redis:${var.container-tag}"
          image_pull_policy = "Always"
          command           = ["redis-server", "--bind", "0.0.0.0", "--loglevel", "verbose"]
          volume_mount {
            name       = "redis-data"
            mount_path = "/usr/share/redis/data"
          }
          resources {
            requests = {
              memory = "1Gi"
              cpu    = 0.25
            }
            limits = {
              memory = "2Gi"
              cpu    = 0.8
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "redis" {
  metadata {
    name      = "redis"
    namespace = var.namespace
    labels = {
      app = "redis"
    }
  }
  spec {
    cluster_ip = var.ip-address
    port {
      name     = "6379-to-6379-tcp"
      protocol = "TCP"
      port     = 6379
    }
    selector = {
      app = "redis"
    }
    type = "ClusterIP"
  }
}

