
resource "kubernetes_deployment" "wallet" {
  provider   = kubernetes
  metadata {
    name      = "gcp-marketplace-integration"
    namespace = var.namespace
    labels = {
      app = "wallet"
    }
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "wallet"
      }
    }
    template {
      metadata {
        labels = {
          app = "wallet"
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
          name              = "gcp-marketplace-integration"
          image             = "zmlp/wallet:${var.container-tag}"
          image_pull_policy = "Always"
          command = ["sh", "/applications/wallet/start-marketplace-tools.sh"]
          resources {
            limits {
              memory = "1Gi"
              cpu    = 1
            }
            requests {
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
            name  = "ZMLP_API_URL"
            value = var.zmlp-api-url
          }
          env {
            name  = "SMTP_PASSWORD"
            value = var.smtp-password
          }
          env {
            name  = "GOOGLE_OAUTH_CLIENT_ID"
            value = var.google-oauth-client-id
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
            name = "GOOGLE_CLOUD_PROJECT"
            value = var.marketplace-project
          }
          env {
            name = "MARKETPLACE_SUBSCRIPTION"
            value = var.marketplace-subscription
          }
          env {
            name = "GOOGLE_CREDENTIALS"
            value = "{\"type\": \"service_account\", \"project_id\": \"zorroa-marketplace\", \"private_key_id\": \"85dcb6e4dbef8d3aab9d0ac2bca4a72e4e09da40\", \"private_key\": \"-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDhmN1Y5htXiDdA\nktMFZeSeBJXQ80GDHzUFBxZVA/28EmiqvlwraEQsSQ28MA9UDfSdL7ostV//qjWN\nbu2buU8z2/hpuThp8/jLGrZ1Xb0/eN9THhnWWwi4Q6RkR83rXRgGZDe697p+4TiD\nG0FKtb+Cfg3kBRIYL4Pk5FOR8z+QKB0r7ULPnohSd1EGPUvhKmlpme2x79G9rF6H\nsoBfhi+8wYu+kUdkNsbUwol3bP8NSabHuVT/9Pd5aflh6DrcTM1LEJaagMj3wwa6\nu1bn09VcsZUEXHkl69mO9DOzHhPu8QGyojp0sYvycIzzEXMFc3S/W5EiL40yaLbw\nXoyD1kGZAgMBAAECggEABF2qNM7X1OzoZNRylfFje98UsQD6Ub6vonO8WkoStwLm\np1h/JRCGRiZcZa/yQ2E/TH8lf4gAmSUgR+H27CXQtG0lYPUt6lA9yZfXrGyQ6fQC\nbS7nVMuUXwF1CeqNhLdRu2aMfsnNxTvBqSVsCb9UiceAlLOQumNnK+PBxpWd7oax\nXdeqEaDBJ1wM3R7fzlmKtpXVm38jemU/OZ18VYNz1NBWVXulsIWrY0dfyK7eWRXr\ngvGuERjqvPYGydew130tKzWQ8sqJcJ3JKb4Hqd9p5cycfQ/ymPaXiynsfOVVgy2B\nFx4h7d+37c2ftJtGg8fI/0cW0SbiCazxhhHzQTrJoQKBgQD4I8cP3sTTMja9R1Hi\n3ibZOyqDe++F9Cq62T/lj7kuAclfPk+Tnokla5vHmwdnkkoB1YlPmG4EQpf7BCQu\n5rVxOzhai/RQwhrtUlakXVFiOee+n67zEyYb6AaFKQKfDgoWhwjKCb99Q/eiWsk/\nbkHUb7+P0M7cYvSmuskE7iC0PwKBgQDovkifnQ8Nyj28v2g9+U1BKD0CGfcQGrf3\nIgHcqNdq4j+UyCXcC22eHTKPa2NNlXa0bBvxd4HUF7y97Unm3IckNoshJ65lEMxC\nHsZK2BRh76pycBRQuhf9o6az/p9hRiWHfT0v43iGbMLz1UoORO+zAfWypLDol8OI\ntRG44EI0JwKBgBJyVnIPlYI90WPw0V2UNj8f7uEnbX7/z81kKSPfs1yANYAnGbNX\nrcpiXYpRNBO3BFlujm6kXkliyfmXLTlvXT21sVLJ5Le8NlQ+MsK1TT+IHnpFVLQt\nUD/y18k+azt3x7g1AapDX0DotJgVN7DHeY1ZYVxHoZkwog9jA16idTvxAoGBAMpQ\nxn6BH5nkKOCPHkVpFf/Cw6209nA4WZxpImc3GfLec1iY11g9Uu90AARt3CeP+l0B\n8RRoxyGf9MDAqVIWdx9CjpSmktFl1bjQWZyr0qQDmw3ZYb9+1OX1wS6L8u2y9WKe\nunaLbLSljJ461SIsSJU56eN9iA7YBfArPhmQT9xXAoGBAJ5cChfD3k7KBmvHBV4/\nBhBbWJkLLgIxu5C6Zdc1bltimaLWUVWkrW/CatCcjh9fTfDnjadajXHDuQRq+Noz\nkmQFDzndaD6VLfmTVRLhBTyg92NTlA2I1lQiggiylULVakxA5ls5E7HvafA/vAE+\n65URnKzLE8HeWgDmh3dTHJtc\n-----END PRIVATE KEY-----\n\", \"client_email\": \"zorroa-marketplace-codelab@zorroa-marketplace.iam.gserviceaccount.com\", \"client_id\": \"111801712545622279591\", \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\", \"token_uri\": \"https://oauth2.googleapis.com/token\", \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\", \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/zorroa-marketplace-codelab%40zorroa-marketplace.iam.gserviceaccount.com\" }"
          }
        }
      }
    }
  }
}
