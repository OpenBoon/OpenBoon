output "ip-address" {
  value = kubernetes_service.analyst.spec[0].cluster_ip
}

output "shared-key" {
  value = random_password.analyst-shared-key.result
}
