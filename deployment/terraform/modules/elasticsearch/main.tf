locals {
  es-cluster-size = "${min(var.es-cluster-size, 5)}"
  es-hosts = [
    "elasticsearch-0.elasticsearch.${var.namespace}.svc.cluster.local",
    "elasticsearch-1.elasticsearch.${var.namespace}.svc.cluster.local",
    "elasticsearch-2.elasticsearch.${var.namespace}.svc.cluster.local",
    "elasticsearch-3.elasticsearch.${var.namespace}.svc.cluster.local",
    "elasticsearch-4.elasticsearch.${var.namespace}.svc.cluster.local"]
  es-host-string = "${join(",", slice(local.es-hosts, 0, local.es-cluster-size))}"
}

resource "google_container_node_pool" "elasticsearch" {
  name = "${var.node-pool-name}"
  cluster = "${var.container-cluster-name}"
  initial_node_count = "${local.es-cluster-size}"
  autoscaling {
    max_node_count = 6
    min_node_count = 1
  }
  management {
    auto_repair = true
    auto_upgrade = true
  }
  node_config {
    machine_type = "custom-2-8192"
    oauth_scopes = [
      "https://www.googleapis.com/auth/compute",
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
    ]
    taint {
      effect = "NO_SCHEDULE"
      key = "elasticsearch"
      value = "false"
    }
    labels {
      type = "elasticsearch"
      namespace = "${var.namespace}"
    }
  }
}

resource "kubernetes_storage_class" "elasticsearch" {
  "metadata" {
    name = "${var.storage-class-name}"
  }
  storage_provisioner = "kubernetes.io/gce-pd"
  parameters {
    type = "pd-ssd"
  }
}

resource "kubernetes_stateful_set" "elasticsearch" {
  provider = "kubernetes"
  metadata {
    name = "elasticsearch"
    namespace = "${var.namespace}"
    labels {
      app = "elasticsearch"
      service = "elasticsearch"
    }
  }

  spec {
    update_strategy {
      type = "RollingUpdate"
    }
    service_name = "elasticsearch"
    replicas = "${local.es-cluster-size}"
    selector {
      match_labels {
        app = "elasticsearch"
      }
    }
    volume_claim_template {
      metadata {
        name = "elasticsearch-data"
      }
      spec {
        access_modes = ["ReadWriteOnce"]
        storage_class_name = "${kubernetes_storage_class.elasticsearch.metadata.0.name}"
        resources {
          requests {
            storage = "100Gi"
          }
        }
      }
    }
    template {
      metadata {
        labels {
          app = "elasticsearch"
        }
      }
      spec {
        termination_grace_period_seconds = 300
        node_selector {
          type = "elasticsearch"
          namespace = "${var.namespace}"
        }
        toleration {
          key = "elasticsearch"
          operator = "Equal"
          value = "false"
          effect = "NoSchedule"
        }
        init_container {
          name = "set-max-map-count"
          image = "busybox:1.27.2"
          command = ["sysctl", "-w", "vm.max_map_count=262144"]
          security_context {
            privileged = true
            allow_privilege_escalation = true
          }
        }
        init_container {
          name = "set-ulimit"
          image = "busybox:1.27.2"
          command = ["sh", "-c", "ulimit -n 65536"]
          security_context {
            privileged = true
          }
        }
        init_container {
          name = "chown-data-dir"
          image_pull_policy = "Always"
          image = "zmlp/elasticsearch:${var.container-tag}"
          command = ["chown", "-v", "elasticsearch:elasticsearch", "/usr/share/elasticsearch/data"]
          volume_mount {
            name = "elasticsearch-data"
            mount_path = "/usr/share/elasticsearch/data"
          }
        }
        image_pull_secrets {
          name = "${var.image-pull-secret}"
        }
        container {
          name = "elasticsearch"
          image = "zmlp/elasticsearch:${var.container-tag}"
          image_pull_policy = "Always"
          env = [
            {
              name = "ES_JAVA_OPTS"
              value = "-Xms5g -Xmx5g"
            },
            {
              name = "cluster.name"
              value = "elasticsearch-cluster"
            },
            {
              name = "discovery.zen.ping.unicast.hosts"
              value = "${local.es-host-string}"
            },
            {
              name = "node.name"
              value_from {
                field_ref {
                  field_path = "metadata.name"
                }
              }
            }

          ]
          volume_mount {
            name = "elasticsearch-data"
            mount_path = "/usr/share/elasticsearch/data"
          }
          resources {
            requests {
              memory = "4Gi"
            }
            limits {
              memory = "7Gi"
            }
          }
        }
      }
    }
  }
}


resource "kubernetes_service" "elasticsearch" {
  "metadata" {
    name = "elasticsearch"
    namespace = "${var.namespace}"
    labels {
      app = "elasticsearch"
    }
  }
  "spec" {
    cluster_ip = "None"
    port {
      name = "9200-to-9200-tcp"
      protocol = "TCP"
      port = 9200
    }
    port {
      name = "9300-to-9300-tcp"
      protocol = "TCP"
      port = 9300
    }
    selector {
      app = "elasticsearch"
    }
    type = "ClusterIP"
  }
}
