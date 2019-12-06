output "ip-address" {
  value = "${kubernetes_service.archivist.spec.0.cluster_ip}"
}

