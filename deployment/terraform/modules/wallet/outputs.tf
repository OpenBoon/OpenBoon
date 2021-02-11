output "pg_password" {
  value     = random_string.sql-password.result
  sensitive = true
}

