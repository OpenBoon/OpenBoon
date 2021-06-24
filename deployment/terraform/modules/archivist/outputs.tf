output "ip-address" {
  value = kubernetes_service.archivist.spec[0].cluster_ip
}

output "monitor-password" {
  value     = random_string.monitor-password.result
  sensitive = true
}

output "data-bucket-name" {
  value = google_storage_bucket.data.name
}

output "pubsub-topic-name-builds" {
  value = google_pubsub_topic.cloud-builds.name
}

output "pubsub-topic-name-models" {
  value = google_pubsub_topic.model-events.name
}
