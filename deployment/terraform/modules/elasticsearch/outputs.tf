output "ip-address" {
  value = "${kubernetes_service.elasticsearch-master.spec.0.cluster_ip}"
}
