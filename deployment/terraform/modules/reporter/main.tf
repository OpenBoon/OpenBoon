resource "google_monitoring_dashboard" "zvi" {
  dashboard_json = <<EOF
{
  "displayName": "ZVI Metrics",
  "gridLayout": {
    "columns": "2",
    "widgets": [
      {
        "title": "Job Queue - Analysts",
        "xyChart": {
          "chartOptions": {
            "mode": "COLOR"
          },
          "dataSets": [
            {
              "minAlignmentPeriod": "60s",
              "plotType": "LINE",
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "aggregation": {
                    "perSeriesAligner": "ALIGN_MEAN"
                  },
                  "filter": "metric.type=\"custom.googleapis.com/zmlp/desired-analyst-count\"",
                  "secondaryAggregation": {}
                },
                "unitOverride": "1"
              }
            },
            {
              "minAlignmentPeriod": "60s",
              "plotType": "LINE",
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "aggregation": {
                    "perSeriesAligner": "ALIGN_MEAN"
                  },
                  "filter": "metric.type=\"custom.googleapis.com/zmlp/current-analyst-count\"",
                  "secondaryAggregation": {}
                },
                "unitOverride": "1"
              }
            }
          ],
          "timeshiftDuration": "0s",
          "yAxis": {
            "label": "y1Axis",
            "scale": "LINEAR"
          }
        }
      },
      {
        "title": "Job Queue - Tasks",
        "xyChart": {
          "chartOptions": {
            "mode": "COLOR"
          },
          "dataSets": [
            {
              "minAlignmentPeriod": "60s",
              "plotType": "LINE",
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "aggregation": {
                    "perSeriesAligner": "ALIGN_MEAN"
                  },
                  "filter": "metric.type=\"custom.googleapis.com/zmlp/total-pending-tasks\"",
                  "secondaryAggregation": {}
                },
                "unitOverride": "1"
              }
            },
            {
              "minAlignmentPeriod": "60s",
              "plotType": "LINE",
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "aggregation": {
                    "perSeriesAligner": "ALIGN_MEAN"
                  },
                  "filter": "metric.type=\"custom.googleapis.com/zmlp/max-running-tasks\"",
                  "secondaryAggregation": {}
                },
                "unitOverride": "1"
              }
            }
          ],
          "timeshiftDuration": "0s",
          "yAxis": {
            "label": "y1Axis",
            "scale": "LINEAR"
          }
        }
      }
    ]
  }
}

EOF
}

resource "kubernetes_deployment" "reporter" {
  metadata {
    name      = "reporter"
    namespace = var.namespace
    labels = {
      app = "reporter"
    }
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "reporter"
      }
    }
    template {
      metadata {
        labels = {
          app = "reporter"
        }
      }
      spec {
        node_selector = {
          type = "analyst"
        }
        image_pull_secrets {
          name = var.image-pull-secret
        }
        toleration {
          key      = "analyst"
          operator = "Equal"
          value    = "false"
          effect   = "NoSchedule"
        }
        container {
          name              = "reporter"
          image             = "boonai/reporter:${var.container-tag}"
          image_pull_policy = "Always"
          env {
            name  = "PROJECT_ID"
            value = var.project
          }
          env {
            name  = "BOONAI_API_URL"
            value = var.zmlp-api-url
          }
          env {
            name  = "INCEPTION_KEY_B64"
            value = var.inception-key-b64
          }
          env {
            name  = "COLLECTION_INTERVAL"
            value = var.collection-interval
          }
          env {
            name  = "MONITOR_PASSWORD"
            value = var.monitor-password
          }
        }
      }
    }
  }
}
