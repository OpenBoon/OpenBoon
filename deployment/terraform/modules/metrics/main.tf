resource "random_string" "sql-password" {
  length  = 16
  special = false
}

resource "random_password" "django-secret-key" {
  length = 50
}

resource "google_sql_database" "metrics" {
  lifecycle {
    prevent_destroy = true
  }
  depends_on = [google_sql_user.metrics]
  name       = var.database-name
  instance   = var.sql-instance-name
}

resource "google_sql_user" "metrics" {
  name     = var.database-user
  instance = var.sql-instance-name
  password = random_string.sql-password.result
}

resource "kubernetes_deployment" "metrics" {
  provider   = kubernetes
  depends_on = [google_sql_user.metrics]
  lifecycle {
    ignore_changes = [spec[0].replicas]
  }
  metadata {
    name      = "metrics"
    namespace = var.namespace
    labels = {
      app = "metrics"
    }
    annotations = {
      "terraform/sql-service-account-key-date" = var.sql-service-account-key-date
    }
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "metrics"
      }
    }
    template {
      metadata {
        labels = {
          app = "metrics"
        }
      }
      spec {
        node_selector = {
          type = "default"
        }
        image_pull_secrets {
          name = var.image-pull-secret
        }
        volume {
          name = "cloudsql-instance-credentials"
          secret {
            secret_name = "cloud-sql-sa-key"
          }
        }
        container {
          name    = "cloudsql-proxy"
          image   = "gcr.io/cloudsql-docker/gce-proxy:1.11"
          command = ["/cloud_sql_proxy", "-instances=${var.sql-connection-name}=tcp:5432", "-credential_file=/secrets/cloudsql/credentials.json"]
          security_context {
            run_as_user                = 2
            privileged                 = false
            allow_privilege_escalation = false
          }
          volume_mount {
            name       = "cloudsql-instance-credentials"
            mount_path = "/secrets/cloudsql"
            read_only  = true
          }
          resources {
            limits = {
              memory = "512Mi"
              cpu    = 0.5
            }
            requests = {
              memory = "256Mi"
              cpu    = 0.2
            }
          }
        }
        container {
          name              = "metrics"
          image             = "boonai/metrics:${var.container-tag}"
          image_pull_policy = "Always"
          liveness_probe {
            initial_delay_seconds = 30
            period_seconds        = 5
            http_get {
              scheme = "HTTP"
              path   = "/api/v1/health/"
              port   = "80"
            }
            timeout_seconds = 5
          }
          readiness_probe {
            initial_delay_seconds = 5
            period_seconds        = 5
            failure_threshold     = 10
            http_get {
              scheme = "HTTP"
              path   = "/api/v1/health/"
              port   = "80"
            }
          }
          port {
            container_port = "80"
          }
          resources {
            limits = {
              memory = "2Gi"
              cpu    = 2
            }
            requests = {
              memory = "256Mi"
              cpu    = 1
            }
          }
          env {
            name  = "DB_BACKEND"
            value = "postgres"
          }
          env {
            name  = "PG_DB_HOST"
            value = "localhost"
          }
          env {
            name  = "PG_DB_PASSWORD"
            value = random_string.sql-password.result
          }
          env {
            name  = "PG_DB_PORT"
            value = "5432"
          }
          env {
            name  = "SECRET_KEY"
            value = random_password.django-secret-key.result
          }
          env {
            name  = "DEBUG"
            value = var.debug
          }
          env {
            name  = "ENVIRONMENT"
            value = var.environment
          }
          env {
            name  = "BROWSABLE"
            value = var.browsable
          }
          env {
            name  = "SUPERUSER_EMAIL"
            value = var.superuser-email
          }
          env {
            name  = "SUPERUSER_PASSWORD"
            value = var.superuser-password
          }
          env {
            name  = "SUPERUSER_FIRST_NAME"
            value = var.superuser-first-name
          }
          env {
            name  = "SUPERUSER_LAST_NAME"
            value = var.superuser-last-name
          }
          env {
            name  = "DJANGO_LOG_LEVEL"
            value = var.django-log-level
          }
          env {
            name  = "LOG_REQUESTS"
            value = var.log-requests
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "metrics" {
  metadata {
    name      = "metrics-service"
    namespace = var.namespace
    labels = {
      app = "metrics"
    }
  }
  spec {
    cluster_ip = var.ip-address
    port {
      name     = "http"
      protocol = "TCP"
      port     = 80
    }
    selector = {
      app = "metrics"
    }
    type = "ClusterIP"
  }
}

resource "kubernetes_horizontal_pod_autoscaler" "metrics" {
  provider = kubernetes
  metadata {
    name      = "metrics-hpa"
    namespace = var.namespace
    labels = {
      app = "metrics"
    }
  }
  spec {
    max_replicas = 1
    min_replicas = 1
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = "metrics"
    }
    target_cpu_utilization_percentage = 80
  }
  lifecycle {
    ignore_changes = [spec[0].max_replicas, spec[0].min_replicas]
  }
}
