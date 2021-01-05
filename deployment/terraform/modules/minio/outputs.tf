output "access-key" {
  value = random_string.access_key.result
}

output "secret-key" {
  value     = random_string.secret_key.result
  sensitive = true
}

output "ip-address" {
  value = kubernetes_service.minio.spec[0].cluster_ip
}

