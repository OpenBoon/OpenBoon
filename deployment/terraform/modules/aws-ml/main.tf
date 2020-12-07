resource "aws_s3_bucket" "ml" {
  bucket = "${var.environment}-ml-staging"
  acl    = "private"
}

resource "aws_iam_role" "ml-user" {
  name               = "${var.environment}-ml-user"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "rekognition-full-access" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonRekognitionFullAccess"
  role       = aws_iam_role.ml-user.name
}

resource "aws_iam_role_policy_attachment" "rekognition-service" {
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRekognitionServiceRole"
  role       = aws_iam_role.ml-user.name
}
