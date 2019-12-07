//resource "random_string" "sql-password" {
//  length = 16
//  special = false
//}

resource "google_sql_user" "auth-server" {
  name     = "zorroa"
  instance = "${var.sql-instance-name}"
  password = "zorroa"
}

resource "kubernetes_deployment" "auth-server" {
  provider = "kubernetes"
  depends_on = ["google_sql_user.auth-server"]
  metadata {
    name = "auth-server"
    namespace = "${var.namespace}"
    labels {
      app = "auth-server"
    }
  }
  spec {
    replicas = 2
    selector {
      match_labels {
        app = "auth-server"
      }
    }
    template {
      metadata {
        labels {
          app = "auth-server"
        }
      }
      spec {
        node_selector {
          type = "default"
        }
        image_pull_secrets {
          name = "${var.image-pull-secret}"
        }
        volume {
          name = "cloudsql-instance-credentials"
          secret {
            secret_name = "cloud-sql-sa-key"
          }
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
              cpu = 0.5
            }
            requests {
              memory = "256Mi"
              cpu = 0.2
            }
          }
        }
        container {
          name = "auth-server"
          image = "zmlp/authserver:${var.container-tag}"
          image_pull_policy = "Always"
//          liveness_probe = {
//            initial_delay_seconds = 30
//            period_seconds = 5
//            http_get {
//              scheme = "HTTP"
//              path = "/health/"
//              port = "80"
//            }
//          }
//          readiness_probe = {
//            initial_delay_seconds = 30
//            period_seconds = 30
//            http_get {
//              scheme = "HTTP"
//              path = "/health/"
//              port = "80"
//            }
//          }
          port {
            container_port = "80"
          }
          resources {
            limits {
              memory = "512Mi"
              cpu = 0.5
            }
            requests {
              memory = "256Mi"
              cpu = 0.2
            }
          }
//          env = [
//            {
//              name = "PG_HOST"
//              value = "localhost"
//            },
//            {
//              name = "PG_PASSWORD"
//              value = "${random_string.sql-password.result}"
//            }
//          ]
        }
      }
    }
  }
}

resource "kubernetes_service" "auth-server" {
  "metadata" {
    name = "auth-server-service"
    namespace = "${var.namespace}"
    labels {
      app = "auth-server"
    }
  }
  "spec" {
    cluster_ip = "${var.ip-address}"
    port {
      name = "http"
      protocol = "TCP"
      port = 80
    }
    selector {
      app = "auth-server"
    }
    type = "ClusterIP"
  }
}


resource "kubernetes_horizontal_pod_autoscaler" "auth-server" {
  provider = "kubernetes"
  metadata {
    name = "auth-server-hpa"
    namespace = "${var.namespace}"
    labels {
      app = "auth-server"
    }
  }
  spec {
    max_replicas = 10
    min_replicas = 2
    scale_target_ref {
      api_version = "apps/v1"
      kind = "Deployment"
      name = "auth-server"
    }
    target_cpu_utilization_percentage = 80
  }
}
