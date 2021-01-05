output "ip-address" {
  value = kubernetes_service.analyst.spec[0].cluster_ip
}

output "shared-key" {
  value     = random_string.analyst-shared-key.result
  sensitive = true
}
