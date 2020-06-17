output "ip-address" {
  value = kubernetes_service.elasticsearch-master.spec[0].cluster_ip
}

output "backup-bucket-name" {
  value = google_storage_bucket.elasticsearch.name
}
