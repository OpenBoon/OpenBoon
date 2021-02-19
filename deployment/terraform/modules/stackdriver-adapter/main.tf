resource "kubernetes_namespace" "custom_metrics" {
  metadata {
    name = "custom-metrics"
  }
}

resource "kubernetes_service_account" "custom_metrics_stackdriver_adapter" {
  metadata {
    name      = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"
  }
  automount_service_account_token = true
}

resource "kubernetes_cluster_role_binding" "custom_metrics_system_auth_delegator" {
  metadata {
    name = "custom-metrics:system:auth-delegator"
  }

  subject {
    kind      = "ServiceAccount"
    name      = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "system:auth-delegator"
  }
}

resource "kubernetes_role_binding" "custom_metrics_auth_reader" {
  metadata {
    name      = "custom-metrics-auth-reader"
    namespace = "kube-system"
  }

  subject {
    kind      = "ServiceAccount"
    name      = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "Role"
    name      = "extension-apiserver-authentication-reader"
  }
}

resource "kubernetes_cluster_role_binding" "custom_metrics_resource_reader" {
  metadata {
    name = "custom-metrics-resource-reader"
  }

  subject {
    kind      = "ServiceAccount"
    name      = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "view"
  }
}

resource "kubernetes_deployment" "custom_metrics_stackdriver_adapter" {
  metadata {
    name      = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"

    labels = {
      k8s-app = "custom-metrics-stackdriver-adapter"

      run = "custom-metrics-stackdriver-adapter"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        k8s-app = "custom-metrics-stackdriver-adapter"

        run = "custom-metrics-stackdriver-adapter"
      }
    }

    template {
      metadata {
        labels = {
          k8s-app = "custom-metrics-stackdriver-adapter"

          "kubernetes.io/cluster-service" = "true"

          run = "custom-metrics-stackdriver-adapter"
        }
      }

      spec {
        automount_service_account_token = true
        container {
          name    = "pod-custom-metrics-stackdriver-adapter"
          image   = "gcr.io/google-containers/custom-metrics-stackdriver-adapter:v0.10.2"
          command = ["/adapter", "--use-new-resource-model=true"]

          resources {
            limits = {
              cpu    = "250m"
              memory = "200Mi"
            }

            requests = {
              cpu    = "250m"
              memory = "200Mi"
            }
          }

          image_pull_policy = "Always"
        }

        service_account_name = "custom-metrics-stackdriver-adapter"
      }
    }
  }
}

resource "kubernetes_service" "custom_metrics_stackdriver_adapter" {
  metadata {
    name      = "custom-metrics-stackdriver-adapter"
    namespace = "custom-metrics"

    labels = {
      k8s-app = "custom-metrics-stackdriver-adapter"

      "kubernetes.io/cluster-service" = "true"

      "kubernetes.io/name" = "Adapter"

      run = "custom-metrics-stackdriver-adapter"
    }
  }

  spec {
    port {
      protocol    = "TCP"
      port        = 443
      target_port = "443"
    }

    selector = {
      k8s-app = "custom-metrics-stackdriver-adapter"

      run = "custom-metrics-stackdriver-adapter"
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_api_service" "v_1_beta_1_custom" {
  metadata {
    name = "v1beta1.custom.metrics.k8s.io"
  }

  spec {
    service {
      namespace = "custom-metrics"
      name      = "custom-metrics-stackdriver-adapter"
    }

    group                    = "custom.metrics.k8s.io"
    version                  = "v1beta1"
    insecure_skip_tls_verify = true
    group_priority_minimum   = 100
    version_priority         = 100
  }
}

resource "kubernetes_api_service" "v_1_beta_2_custom" {
  metadata {
    name = "v1beta2.custom.metrics.k8s.io"
  }

  spec {
    service {
      namespace = "custom-metrics"
      name      = "custom-metrics-stackdriver-adapter"
    }

    group                    = "custom.metrics.k8s.io"
    version                  = "v1beta2"
    insecure_skip_tls_verify = true
    group_priority_minimum   = 100
    version_priority         = 200
  }
}

resource "kubernetes_api_service" "v_1_beta_1_external" {
  metadata {
    name = "v1beta1.external.metrics.k8s.io"
  }

  spec {
    service {
      namespace = "custom-metrics"
      name      = "custom-metrics-stackdriver-adapter"
    }

    group                    = "external.metrics.k8s.io"
    version                  = "v1beta1"
    insecure_skip_tls_verify = true
    group_priority_minimum   = 100
    version_priority         = 100
  }
}

resource "kubernetes_cluster_role" "external_metrics_reader" {
  metadata {
    name = "external-metrics-reader"
  }

  rule {
    verbs      = ["list", "get", "watch"]
    api_groups = ["external.metrics.k8s.io"]
    resources  = ["*"]
  }
}

resource "kubernetes_cluster_role_binding" "external_metrics_reader" {
  metadata {
    name = "external-metrics-reader"
  }

  subject {
    kind      = "ServiceAccount"
    name      = "horizontal-pod-autoscaler"
    namespace = "kube-system"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "external-metrics-reader"
  }
}
