
resource "kubernetes_deployment" "project-reaper" {
  provider = kubernetes
  metadata {
    name      = "project-reaper"
    namespace = var.namespace
    labels = {
      app = "project-reaper"
    }
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "project-reaper"
      }
    }
    template {
      metadata {
        labels = {
          app = "project-reaper"
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
          name              = "project-reaper"
          image             = "boonai/wallet:${var.container-tag}"
          image_pull_policy = "Always"
          command           = ["python3", "-u", "/applications/wallet/app/manage.py", "projectreaper"]
          resources {
            limits = {
              memory = "1Gi"
              cpu    = 1
            }
            requests = {
              memory = "256Mi"
              cpu    = 0.5
            }
          }
          env {
            name  = "PG_HOST"
            value = "localhost"
          }
          env {
            name  = "PG_PASSWORD"
            value = var.pg_password
          }
          env {
            name  = "BOONAI_API_URL"
            value = var.zmlp-api-url
          }
          env {
            name  = "ENVIRONMENT"
            value = var.environment
          }
          env {
            name  = "ENABLE_SENTRY"
            value = "true"
          }
          env {
            name  = "INCEPTION_KEY_B64"
            value = var.inception-key-b64
          }
          env {
            name  = "FQDN"
            value = var.fqdn
          }
          env {
            name  = "SA_KEY_DATE"
            value = var.sql-service-account-key-date
          }
        }
      }
    }
  }
}
