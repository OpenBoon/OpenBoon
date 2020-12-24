output "access-key" {
  value = random_string.access_key.result
}

output "secret-key" {
  value = random_password.secret_key.result
}

output "ip-address" {
  value = kubernetes_service.minio.spec[0].cluster_ip
}

