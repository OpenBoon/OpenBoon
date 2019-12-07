resource "google_project_service" "service-usage" {
  service = "serviceusage.googleapis.com"
  disable_on_destroy = false
}

## GCS Buckets and Configuration Files
//resource "google_storage_bucket" "data" {
//  name     = "${var.project}-${var.data-bucket-name}"
//  storage_class = "REGIONAL"
//  location = "${var.region}"
//  cors {
//    origin = ["*"]
//    method = ["GET"]
//  }
//}

resource "google_storage_bucket" "configuration" {
  name     = "${var.project}-${var.config-bucket-name}"
  storage_class = "REGIONAL"
  location = "${var.region}"
  versioning {
    enabled = true
  }
}

resource "random_string" "sql-password" {
  length = 16
  special = false
}

resource "google_storage_bucket_object" "archivist-properties" {
  bucket = "${google_storage_bucket.configuration.name}"
  name = "zorroa-archivist-config/application.properties"
  content = <<EOF
archivist.debug-mode.enabled = true
spring.datasource.url = jdbc:postgresql://localhost/${var.database-name}?currentSchema=zorroa&useSSL=false&socketFactoryArg=${var.sql-connection-name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory&user=${var.database-user}&password=${random_string.sql-password.result}
spring.datasource.username=${var.database-user}
spring.datasource.password=${random_string.sql-password.result}
management.endpoints.password=${var.monitor-password}
archivist.storage.create-bucket = True
EOF
}

resource "google_storage_bucket_object" "inception-key" {
  name = "inception-key.json"
  bucket = "${google_storage_bucket.configuration.name}"
  content = <<EOF
{
    "name": "admin-key",
    "projectId": "00000000-0000-0000-0000-000000000000",
    "keyId": "4338a83f-a920-40ab-a251-a123b17df1ba",
    "sharedKey": "pcekjDV_ipSMXAaBqqtq6Jwy5FAMnjehUQrMEhbG8W01giVqVLfEN9FdMIvzu0rb",
    "permissions": [
        "SuperAdmin", "ProjectAdmin", "AssetsRead", "AssetsImport"
    ]
}
EOF
}


## SQL Instance
resource "google_project_service" "sqladmin" {
  service = "sqladmin.googleapis.com"
  disable_on_destroy = false
  depends_on = ["google_project_service.service-usage"]
}



resource "google_sql_user" "users" {
  name     = "${var.database-user}"
  instance = "${var.sql-instance-name}"
  password = "${random_string.sql-password.result}"
}


## K8S Deployment
//resource "google_container_node_pool" "archivist" {
//  name = "${var.node-pool-name}"
//  cluster = "${var.container-cluster-name}"
//  initial_node_count = 1
//  autoscaling {
//    max_node_count = "${var.maximum-nodes}"
//    min_node_count = "${var.minimum-nodes}"
//  }
//  management {
//    auto_repair = true
//    auto_upgrade = true
//  }
//  node_config {
//    machine_type = "custom-2-4096"
//    oauth_scopes = [
//      "compute-rw",
//      "storage-rw",
//      "logging-write",
//      "https://www.googleapis.com/auth/pubsub",
//      "https://www.googleapis.com/auth/devstorage.full_control",
//      "https://www.googleapis.com/auth/devstorage.read_only",
//      "https://www.googleapis.com/auth/monitoring",
//      "https://www.googleapis.com/auth/sqlservice.admin"
//    ]
//    labels {
//      type = "archivist"
//      namespace = "${var.namespace}"
//    }
//  }
//}

//resource "kubernetes_config_map" "archivist" {
//  metadata {
//    name = "archivist-config"
//    namespace = "${var.namespace}"
//    labels {
//      app = "archivist"
//    }
//  }
//  data {
//    GCS_CONFIGURATION_BUCKET = "${google_storage_bucket.configuration.name}/zorroa-archivist-config"
//    ZORROA_USER = "admin"
//    ZORROA_ARCHIVIST_EXT = "${var.extensions}"
//  }
//}

resource "kubernetes_deployment" "archivist" {
  provider = "kubernetes"
  metadata {
    name = "archivist"
    namespace = "${var.namespace}"
    labels {
      app = "archivist"
    }
  }
  spec {
    selector {
      match_labels {
        app = "archivist"
      }
    }
    strategy {
      type = "${var.rollout-strategy}"
    }
    template {
      metadata {
        labels {
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
        node_selector {
          type = "default"
        }
        image_pull_secrets {
          name = "${var.image-pull-secret}"
        }
        container {
          name = "cloudsql-proxy"
          image = "gcr.io/cloudsql-docker/gce-proxy:1.11"
          command = ["/cloud_sql_proxy", "-instances=${var.sql-connection-name}=tcp:5432", "-credential_file=/secrets/cloudsql/credentials.json"]
          security_context {
            run_as_user = 2
            privileged = false
            allow_privilege_escalation = false
          }
          volume_mount {
            name = "cloudsql-instance-credentials"
            mount_path = "/secrets/cloudsql"
            read_only = true
          }
          resources {
            limits {
              memory = "512Mi"
              cpu = 0.2
            }
            requests {
              memory = "256Mi"
              cpu = 0.1
            }
          }
        }
        container {
          name = "zorroa-archivist"
          image = "zorroaevi/archivist:${var.container-tag}"
          image_pull_policy = "Always"
          resources {
            limits {
              memory = "1Gi"
              cpu = 0.5
            }
            requests {
              memory = "1Gi"
              cpu = 0.2
            }
          }
          liveness_probe = {
            initial_delay_seconds = 120
            period_seconds = 5
            http_get {
              scheme = "HTTP"
              path = "/actuator/health"
              port = "8080"
            }
          }
          readiness_probe = {
            failure_threshold = 6
            initial_delay_seconds = 30
            period_seconds = 30
            http_get {
              scheme = "HTTP"
              path = "/actuator/health"
              port = "8080"
            }
          }
          env = [
            {
              name = "SENTRY_ENVIRONMENT"
              value = "production"
            },
            {
              name = "ARCHIVIST_STORAGE_ACCESSKEY"
              value = "qwerty123"
            },
            {
              name = "ARCHIVIST_STORAGE_SECRETKEY"
              value = "123qwerty"
            },
            {
              name = "ARCHIVIST_STORAGE_URL"
              value = "gs://${var.project}-${var.data-bucket-name}"
            }
          ]
        }
      }
    }
  }
}

resource "kubernetes_horizontal_pod_autoscaler" "archivist" {
  provider = "kubernetes"
  metadata {
    name = "archivist-hpa"
    namespace = "${var.namespace}"
    labels {
      app = "archivist"
    }
  }
  spec {
    max_replicas = "${var.maximum-replicas}"
    min_replicas = "${var.minimum-replicas}"
    scale_target_ref {
      api_version = "apps/v1"
      kind = "Deployment"
      name = "archivist"
    }
    target_cpu_utilization_percentage = 75
  }
}

resource "kubernetes_service" "archivist" {
  "metadata" {
    name = "archivist-service"
    namespace = "${var.namespace}"
    labels {
      app = "archivist"
    }
  }
  "spec" {
    cluster_ip = "${var.ip-address}"
    port {
      name = "80-to-8080-tcp"
      protocol = "TCP"
      port = 80
      target_port = "8080"
    }
    port {
      name = "443-to-8443-tcp"
      protocol = "TCP"
      port = 443
      target_port = "8443"
    }
    selector {
      app = "archivist"

    }
    type = "ClusterIP"
  }
}

resource "kubernetes_secret" "sql-credentials" {
  metadata {
    name = "sql-credentials"
    namespace = "${var.namespace}"
  }
  data {
    username = "zorroa"
    password = "${random_string.sql-password.result}"
  }
}
