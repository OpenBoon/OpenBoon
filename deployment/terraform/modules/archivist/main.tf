resource "google_project_service" "service-usage" {
  service            = "serviceusage.googleapis.com"
  disable_on_destroy = false
}

## GCS Buckets and Configuration Files
resource "google_storage_bucket" "data" {
  name          = "${var.project}-${var.data-bucket-name}"
  storage_class = "REGIONAL"
  location      = var.region
  cors {
    origin = ["*"]
    method = ["GET"]
  }
}

## SQL Instance
resource "random_string" "sql-password" {
  length  = 16
  special = false
}

resource "google_project_service" "sqladmin" {
  service            = "sqladmin.googleapis.com"
  disable_on_destroy = false
  depends_on         = [google_project_service.service-usage]
}

resource "google_sql_database" "archivist" {
  name     = var.database-name
  instance = var.sql-instance-name
}

resource "google_sql_user" "zorroa" {
  name     = var.database-user
  instance = var.sql-instance-name
  password = random_string.sql-password.result
}

## Service Account For Blob Signing
resource "google_service_account" "archivist" {
  project      = var.project
  account_id   = "zmlp-archivist"
  display_name = "ZMLP Archivist"
}

resource "google_project_iam_custom_role" "archivist" {
  project = var.project
  role_id     = "archivist"
  title       = "ZMLP Archivist Role"
  description = "Role assigned to the service account used by the Archivist."
  permissions = [
    "iam.serviceAccounts.signBlob",
    "storage.buckets.create",
    "storage.buckets.get",
    "storage.buckets.update",
    "storage.objects.create",
    "storage.objects.delete",
    "storage.objects.get",
    "storage.objects.list"
  ]
}

resource "google_project_iam_member" "archivist" {
  project = var.project
  role    = google_project_iam_custom_role.archivist.id
  member  = "serviceAccount:${google_service_account.archivist.email}"
}

resource "google_service_account_key" "archivist" {
  service_account_id = google_service_account.archivist.name
}

resource "kubernetes_secret" "archivist-sa-key" {
  metadata {
    name      = "archivist-sa-key"
    namespace = var.namespace
  }
  data = {
    "credentials.json" = base64decode(google_service_account_key.archivist.private_key)
  }
}

## K8S Deployment
resource "kubernetes_deployment" "archivist" {
  provider = kubernetes
  metadata {
    name      = "archivist"
    namespace = var.namespace
    labels = {
      app = "archivist"
    }
  }
  spec {
    selector {
      match_labels = {
        app = "archivist"
      }
    }
    strategy {
      type = var.rollout-strategy
    }
    template {
      metadata {
        labels = {
          app = "archivist"
        }
      }
      spec {
        volume {
          name = "cloudsql-instance-credentials"
          secret {
            secret_name = "cloud-sql-sa-key"
          }
        }
        volume {
          name = "archivist-credentials"
          secret {
            secret_name = "archivist-sa-key"
          }
        }
        node_selector = {
          type = "default"
        }
        image_pull_secrets {
          name = var.image-pull-secret
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
            limits {
              memory = "512Mi"
              cpu    = 0.5
            }
            requests {
              memory = "256Mi"
              cpu    = 0.2
            }
          }
        }
        container {
          name              = "archivist"
          image             = "zmlp/archivist:${var.container-tag}"
          image_pull_policy = "Always"
          volume_mount {
            name       = "archivist-credentials"
            mount_path = "/secrets/gcs"
            read_only  = true
          }
          resources {
            limits {
              memory = "2Gi"
              cpu    = 0.7
            }
            requests {
              memory = "1Gi"
              cpu    = 0.5
            }
          }
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/monitor/health"
              port   = "8080"
            }
          }
          readiness_probe {
            failure_threshold     = 6
            initial_delay_seconds = 90
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/monitor/health"
              port   = "8080"
            }
          }
          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "gcs"
          }
          env {
            name  = "SPRING_DATASOURCE_URL"
            value = "jdbc:postgresql://localhost/${var.database-name}?currentSchema=zorroa&useSSL=false&cloudSqlInstance=${var.sql-connection-name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory&user=${var.database-user}&password=${random_string.sql-password.result}"
          }
          env {
            name  = "ZMLP_STORAGE_PROJECT_BUCKET"
            value = google_storage_bucket.data.name
          }
          env {
            name  = "ARCHIVIST_ES_URL"
            value = "${var.elasticsearch-url}:9200"
          }
          env {
            name  = "ZMLP_SECURITY_AUTHSERVER_SERVICEKEY"
            value = var.inception-key-b64
          }
          env {
            name  = "ZMLP_SECURITY_AUTHSERVER_URL"
            value = var.auth-server-url
          }
          env {
            name  = "ZMLP_STORAGE_PIPELINE_URL"
            value = var.minio-url
          }
          env {
            name  = "ZMLP_STORAGE_PIPELINE_SECRETKEY"
            value = var.minio-secret-key
          }
          env {
            name  = "ZMLP_STORAGE_PIPELINE_ACCESSKEY"
            value = var.minio-access-key
          }
          env {
            name  = "ANALYST_SHAREDKEY"
            value = "QjZEQzRDQTgtOUUwRC00NUE1LUFCNjktRUYwQTA4ODc4MTM3Cg"
          }
          env {
            name  = "ZMLP_STORAGE_SYSTEM_BUCKET"
            value = var.system-bucket
          }
        }
      }
    }
  }
}

resource "kubernetes_horizontal_pod_autoscaler" "archivist" {
  provider = kubernetes
  metadata {
    name      = "archivist-hpa"
    namespace = var.namespace
    labels = {
      app = "archivist"
    }
  }
  spec {
    max_replicas = var.maximum-replicas
    min_replicas = var.minimum-replicas
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = "archivist"
    }
    target_cpu_utilization_percentage = 75
  }
}

resource "kubernetes_service" "archivist" {
  metadata {
    name      = "archivist-service"
    namespace = var.namespace
    labels = {
      app = "archivist"
    }
  }
  spec {
    cluster_ip = var.ip-address
    port {
      name        = "80-to-8080-tcp"
      protocol    = "TCP"
      port        = 80
      target_port = "8080"
    }
    port {
      name        = "443-to-8443-tcp"
      protocol    = "TCP"
      port        = 443
      target_port = "8443"
    }
    selector = {
      app = "archivist"
    }
    type = "ClusterIP"
  }
}

