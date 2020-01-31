output "ip-address" {
  value = "${kubernetes_service.api-gateway.spec.0.cluster_ip}"
}
