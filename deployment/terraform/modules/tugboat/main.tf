resource "google_project_service" "cloudbuild" {
  service            = "cloudbuild.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_iam_member" "cloudbuilder" {
  project    = var.project
  role       = "roles/run.admin"
  member     = "serviceAccount:${var.project-number}@cloudbuild.gserviceaccount.com"
  depends_on = [google_project_service.cloudbuild]
}

resource "google_pubsub_subscription" "tugboat-model-events" {
  name  = "tugboat-model-events"
  topic = var.pubsub-topic
}


## Service Account For Blob Signing
resource "google_service_account" "tugboat" {
  project      = var.project
  account_id   = "zmlp-tugboat"
  display_name = "ZMLP Tugboat"
}

resource "google_project_iam_custom_role" "tugboat" {
  project     = var.project
  role_id     = "tugboat"
  title       = "ZMLP Tugboat Role"
  description = "Role assigned to the service account used by Tugboat."
  permissions = [
    "storage.objects.get",
    "storage.objects.list",
    "cloudbuild.builds.create",
    "cloudbuild.builds.get",
    "cloudbuild.builds.list",
    "cloudbuild.builds.update",
    "remotebuildexecution.blobs.get",
    "resourcemanager.projects.get",
    "resourcemanager.projects.list"
  ]
}

resource "google_project_iam_member" "tugboat" {
  project    = var.project
  role       = google_project_iam_custom_role.tugboat.id
  member     = "serviceAccount:${google_service_account.tugboat.email}"
  depends_on = [google_project_iam_custom_role.tugboat, google_service_account.tugboat]
}

resource "google_service_account_key" "tugboat" {
  service_account_id = google_service_account.tugboat.name
  keepers = {
    "created_date" : timestamp()
  }
}

resource "kubernetes_secret" "archivist-sa-key" {
  metadata {
    name      = "tugboat-sa-key"
    namespace = var.namespace
  }
  data = {
    "credentials.json" = base64decode(google_service_account_key.tugboat.private_key)
  }
}


resource "kubernetes_deployment" "tugboat" {
  provider = kubernetes
  metadata {
    name = "tugboat"
    labels = {
      app = "tugboat"
    }
  }
  spec {
    selector {
      match_labels = {
        app = "tugboat"
      }
    }
    template {
      metadata {
        labels = {
          app = "tugboat"
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
          name = "service-account"
          secret {
            secret_name = "tugboat-sa-key"
          }
        }
        volume {
          name = "certs"
          host_path {
            path = "/etc/ssl/certs"
          }
        }
        container {
          name  = "tugboat"
          image = "boonai/tugboat:${var.container-tag}"
          env {
            name  = "GCLOUD_PROJECT"
            value = var.project
          }
          env {
            name  = "PORT"
            value = "9393"
          }
          env {
            name = "GOOGLE_APPLICATION_CREDENTIALS"
            value = "/var/run/secret/cloud.google.com/service-account.json"
          }
          volume_mount {
            mount_path = "/var/run/secret/cloud.google.com"
            name = "service-account"
          }
          volume_mount {
            mount_path = "/etc/ssl/certs"
            name = "certs"
          }
          liveness_probe {
            initial_delay_seconds = 120
            period_seconds        = 5
            http_get {
              scheme = "HTTP"
              path   = "/health"
              port   = "9393"
            }
          }
          readiness_probe {
            failure_threshold     = 5
            initial_delay_seconds = 1
            period_seconds        = 30
            http_get {
              scheme = "HTTP"
              path   = "/health"
              port   = "9393"
            }
          }
        }
      }
    }
  }
}
