output "ip-address" {
  value = "${kubernetes_service.officer.spec.0.cluster_ip}"
}
