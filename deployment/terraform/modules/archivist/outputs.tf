output "ip-address" {
  value = kubernetes_service.archivist.spec[0].cluster_ip
}

output "monitor-password" {
  value = random_string.monitor-password.result
}
