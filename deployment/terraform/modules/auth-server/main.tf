## SQl Instance
resource "random_string" "sql-password" {
  length  = 16
  special = false
}

resource "google_sql_user" "auth-server" {
  name     = "zorroa-auth-server"
  instance = var.sql-instance-name
  password = random_string.sql-password.result
}

resource "google_sql_database" "auth" {
  lifecycle {
    prevent_destroy = true
  }
  name     = "zorroa-auth"
  instance = var.sql-instance-name
}

## K8S Deployment
resource "kubernetes_deployment" "auth-server" {
  provider   = kubernetes
  depends_on = [google_sql_user.auth-server]
  lifecycle {
    ignore_changes = [spec[0].replicas]
  }
  metadata {
    name      = "auth-server"
    namespace = var.namespace
    labels = {
      app = "auth-server"
    }
    annotations = {
      "terraform/sql-service-account-key-date" = var.sql-service-account-key-date
    }
  }
  spec {
    replicas = 2
    selector {
      match_labels = {
        app = "auth-server"
      }
    }
    template {
      metadata {
        labels = {
          app = "auth-server"
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
          name              = "auth-server"
          image             = "boonai/authserver:${var.container-tag}"
          image_pull_policy = "Always"
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/monitor/health"
              port   = "9090"
            }
          }
          readiness_probe {
            failure_threshold     = 6
            initial_delay_seconds = 90
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/monitor/health"
              port   = "9090"
            }
          }
          port {
            container_port = "9090"
          }
          resources {
            limits = {
              memory = "1Gi"
              cpu    = 0.5
            }
            requests = {
              memory = "512Mi"
              cpu    = 0.2
            }
          }
          env {
            name  = "SPRING_DATASOURCE_URL"
            value = "jdbc:postgresql://localhost/${google_sql_database.auth.name}?currentSchema=auth&useSSL=false&cloudSqlInstance=${var.sql-connection-name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory&user=${google_sql_user.auth-server.name}&password=${random_string.sql-password.result}"
          }
          env {
            name  = "BOONAI_SECURITY_INCEPTIONKEY"
            value = var.inception-key-b64
          }
          env {
            name  = "SWAGGER_ISPUBLIC"
            value = "false"
          }
          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "gcs"
          }
          env {
            name  = "BOONAI_STORAGE_SYSTEM_BUCKET"
            value = var.system-bucket
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "auth-server" {
  metadata {
    name      = "auth-server-service"
    namespace = var.namespace
    labels = {
      app = "auth-server"
    }
  }
  spec {
    cluster_ip = var.ip-address
    port {
      name        = "http"
      protocol    = "TCP"
      port        = 80
      target_port = 9090
    }
    selector = {
      app = "auth-server"
    }
    type = "ClusterIP"
  }
}

resource "kubernetes_horizontal_pod_autoscaler" "auth-server" {
  provider = kubernetes
  metadata {
    name      = "auth-server-hpa"
    namespace = var.namespace
    labels = {
      app = "auth-server"
    }
  }
  spec {
    max_replicas = 2
    min_replicas = 2
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = "auth-server"
    }
    target_cpu_utilization_percentage = 80
  }
  lifecycle {
    ignore_changes = [spec[0].max_replicas, spec[0].min_replicas]
  }
}

