output "bucket" {
  value = aws_s3_bucket.ml.bucket
}

output "ml-user-role-arn" {
  value = aws_iam_role.ml-role.arn
}

output "ml-user-key" {
  value = aws_iam_access_key.ml-user.id
}

output "ml-user-secret" {
  value = aws_iam_access_key.ml-user.secret
}