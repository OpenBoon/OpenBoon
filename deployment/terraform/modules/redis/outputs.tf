output "ip-address" {
  value = "${kubernetes_service.redis.spec.0.cluster_ip}"
}
