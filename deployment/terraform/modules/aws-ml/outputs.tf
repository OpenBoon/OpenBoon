output "bucket" {
  value = aws_s3_bucket.ml.bucket
}

output "ml-user-role-arn" {
  value = aws_iam_role.ml-user.arn
}
