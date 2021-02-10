resource "aws_s3_bucket" "ml" {
  bucket = "${var.environment}-ml-staging"
  acl    = "private"
}

resource "aws_iam_user" "ml-user" {
  name = "${var.environment}-ml-user"
}

resource "aws_iam_access_key" "ml-user" {
  user = aws_iam_user.ml-user.name
}

resource "aws_iam_role" "ml-role" {
  name = "${var.environment}-ml-role"
}

resource "aws_iam_user_policy_attachment" "rekognition-full-access" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonRekognitionFullAccess"
  user = aws_iam_user.ml-user.name
}

resource "aws_iam_user_policy_attachment" "sqs-full-access" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonSQSFullAccess"
  user = aws_iam_user.ml-user.name
}

resource "aws_iam_user_policy_attachment" "sns-full-access" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonSNSFullAccess"
  user = aws_iam_user.ml-user.name
}

resource "aws_iam_user_policy_attachment" "s3-full-access" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
  user = aws_iam_user.ml-user.name
}

resource "aws_iam_user_policy_attachment" "transcribe-full-access" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonTranscribeFullAccess"
  user = aws_iam_user.ml-user.name
}

resource "aws_iam_user_policy" "ml-user" {
  name = "AwsPassRek"
  user = aws_iam_user.ml-user.name
  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "SNSRek2",
            "Effect": "Allow",
            "Action": "iam:PassRole",
            "Resource": "${aws_iam_role.ml-role.arn}"
        }
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "rekognition-service" {
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRekognitionServiceRole"
  role       = aws_iam_role.ml-role.name
}


