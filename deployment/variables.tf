## Required Variables.
variable "project" {
  description = "Name of the GCP project."
}

variable "terraform-credentials" {
  description = "Contents of a credential json file to use for creating resources."
}

variable "docker-username" {
  description = "Username for Docker Hub user."
}

variable "docker-password" {
  description = "Password for Docker Hub user."
}

variable "docker-email" {
  description = "Email address for Docker Hub user."
}

## Optional Variables
variable "initial-node-count" {
  description = "Initial node count for the default nde pool. This option is here to support older installs. The default node pool should be set to 0 nodes after startup."
  default = 1
}

variable "es-cluster-size" {
  description = "Number of nodes in the Elasticsearch cluster. Maximum is 5."
  default = 5
}

variable "country" {
  description = "GCP country abbreviation (i.e. us, eu)."
  default = "us"
}

variable "region" {
  description = "GCP region (i.e. central1, east1)."
  default = "central1"
}

variable "zone" {
  description = "GCP zone letter (i.e. a, b)."
  default = "a"
}

variable "sql-tier" {
  description = "Cloud SQL tier (i.e. db-custom-1-4096)."
  default = "db-custom-1-4096"
}

variable "container-tag" {
  description = "Docker container tag to use for the services."
  default = "latest"
}

variable "data-bucket-name" {
  description = "Basename of the GCS bucket that will house job data."
  default = "zorroa-data"
}

variable "config-bucket-name" {
  description = "Basename of the GCS bucker that will house config files."
  default = "zorroa-configuration"
}

## Archivist Configuration
variable "archivist-rollout-strategy" {
  description = "Rollout strategy for updating the archivist. Options are Recreate or RollingUpdate"
  default = "RollingUpdate"
}

variable "archivist-minimum-replicas" {
  description = "Minimum number of Archivist pods to run."
  default = 2
}

variable "archivist-maximum-replicas" {
  description = "Maximum number of Archivist pods to run."
  default = 5
}

variable "archivist-minimum-nodes" {
  description = "Minimum number of nodes in the Archivist node pool."
  default = 1
}

variable "archivist-maximum-nodes" {
  description = "Maximum number of nodes in the Archivist node pool."
  default = 5
}

variable "archivist-config-properties" {
  description = "Additional properties to add to the archivist's application.properties file."
  default = ""
}

variable "archivist-extensions" {
  description = "Archivists extension to mount. This is a string of *.jar files comma-separated."
  default = ""
}


## Analyst Configuration
variable "analyst-rollout-strategy" {
  description = "Rollout strategy for updating the analyst. Options are Recreate or RollingUpdate"
  default = "RollingUpdate"
}

variable "analyst-minimum-replicas" {
  description = "Minimum number of analyst pods to run."
  default = 1
}

variable "analyst-maximum-replicas" {
  description = "Maximum number of analyst pods to run."
  default = 100
}

variable "analyst-minimum-nodes" {
  description = "Minimum number of nodes in the analyst node pool."
  default = 1
}

variable "analyst-maximum-nodes" {
  description = "Maximum number of nodes in the analyst node pool."
  default = 1
}

variable "analyst-machine-type" {
  description = "Machine type for analyst node pool nodes."
  default = "custom-6-18176"
}

variable "analyst-memory-request" {
  description = "Memory to request for analyst pods."
  default = "4Gi"
}

variable "analyst-memory-limit" {
  description = "Maximum memory an analyst pod can use."
  default = "8Gi"
}

variable "analyst-cpu-request" {
  description = "CPUs to request for analyst pods"
  default = 2.0
}

variable "analyst-cpu-limit" {
  description = "Maximum CPUs an analsyt can use."
  default = 3.0
}


## Officer Configuration
variable "officer-rollout-strategy" {
  description = "Rollout strategy for updating the analyst. Options are Recreate or RollingUpdate"
  default = "RollingUpdate"
}

variable "officer-minimum-replicas" {
  description = "Minimum number of analyst pods to run."
  default = 1
}

variable "officer-maximum-replicas" {
  description = "Maximum number of analyst pods to run."
  default = 100
}

variable "officer-minimum-nodes" {
  description = "Minimum number of nodes in the analyst node pool."
  default = 1
}

variable "officer-maximum-nodes" {
  description = "Maximum number of nodes in the analyst node pool."
  default = 1
}

variable "officer-machine-type" {
  description = "Machine type for analyst node pool nodes."
  default = "custom-6-18176"
}

variable "officer-memory-request" {
  description = "Memory to request for analyst pods."
  default = "4Gi"
}

variable "officer-memory-limit" {
  description = "Maximum memory an analyst pod can use."
  default = "8Gi"
}

variable "officer-cpu-request" {
  description = "CPUs to request for analyst pods"
  default = 2.0
}

variable "officer-cpu-limit" {
  description = "Maximum CPUs an analsyt can use."
  default = 3.0
}


## Generated Variables
locals {
  region = "${var.country}-${var.region}"
  zone = "${var.country}-${var.region}-${var.zone}"
}
