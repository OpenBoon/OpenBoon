output "bucket" {
  value = aws_s3_bucket.ml.bucket
}

output "ml-user-role-arn" {
  value = aws_iam_role.ml-user.arn
}

output "ml-user-sqs-url" {
  value = aws_sqs_queue.ml-user.id
}

output "ml-user-sqs-arn" {
  value = aws_sqs_queue.ml-user.arn
}

output "ml-user-sns-topic-arn" {
  value = aws_sns_topic.ml-user.arn
}
