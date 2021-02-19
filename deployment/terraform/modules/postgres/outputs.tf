output "instance-name" {
  value = google_sql_database_instance.zmlp.name
}

output "connection-name" {
  value = google_sql_database_instance.zmlp.connection_name
}

output "sql-service-account-key" {
  value     = base64decode(google_service_account_key.cloud-sql-account-key.private_key)
  sensitive = true
}

output "sql-service-account-key-date" {
  value = google_service_account_key.cloud-sql-account-key.valid_after
}

output "ip-address" {
  value = google_sql_database_instance.zmlp.first_ip_address
}

