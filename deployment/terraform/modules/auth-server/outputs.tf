output "ip-address" {
  value = "${kubernetes_service.auth-server.spec.0.cluster_ip}"
}
