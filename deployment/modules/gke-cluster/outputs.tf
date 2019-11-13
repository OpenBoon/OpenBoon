output "name" {
  value = "${google_container_cluster.primary.name}"
}

output "endpoint" {
  value = "${google_container_cluster.primary.endpoint}"
}

output "username" {
  value = "${google_container_cluster.primary.master_auth.0.username}"
}

output "password" {
  sensitive = true
  value = "${google_container_cluster.primary.master_auth.0.password}"
}

output "client_certificate" {
  value = "${base64decode(google_container_cluster.primary.master_auth.0.client_certificate)}"
}

output "client_key" {
  value = "${base64decode(google_container_cluster.primary.master_auth.0.client_key)}"
}

output "cluster_ca_certificate" {
  value = "${base64decode(google_container_cluster.primary.master_auth.0.cluster_ca_certificate)}"
}
