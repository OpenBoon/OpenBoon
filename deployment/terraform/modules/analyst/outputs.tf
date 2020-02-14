output "ip-address" {
  value = kubernetes_service.analyst.spec[0].cluster_ip
}

