output "name" {
  value = google_container_cluster.primary.name
}

output "endpoint" {
  value = google_container_cluster.primary.endpoint
}

output "username" {
  value = google_container_cluster.primary.master_auth[0].username
}

output "password" {
  value     = google_container_cluster.primary.master_auth[0].password
  sensitive = true
}

output "client_certificate" {
  value = base64decode(
    google_container_cluster.primary.master_auth[0].client_certificate,
  )
  sensitive = true
}

output "client_key" {
  value     = base64decode(google_container_cluster.primary.master_auth[0].client_key)
  sensitive = true
}

output "cluster_ca_certificate" {
  value = base64decode(
    google_container_cluster.primary.master_auth[0].cluster_ca_certificate,
  )
}

