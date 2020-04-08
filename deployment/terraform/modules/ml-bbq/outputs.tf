output "ip-address" {
  value = kubernetes_service.ml-bbq.spec[0].cluster_ip
}

