resource "aws_s3_bucket" "ml" {
  bucket = "${var.environment}-ml-staging"
  acl    = "private"
}
