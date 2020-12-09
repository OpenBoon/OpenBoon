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

resource "aws_sns_topic" "ml-user" {
  name = "AmazonRekognition-${var.environment}-ml"
}

resource "aws_sqs_queue" "ml-user" {
  name = "AmazonRekognition-${var.environment}-ml"

}

resource "aws_sqs_queue_policy" "ml-user" {
  queue_url = aws_sqs_queue.ml-user.id
  policy    = <<POLICY
{
  "Version":"2012-10-17",
  "Statement":[
    {
      "Sid":"AmazonRekognitionQueue",
      "Effect":"Allow",
      "Principal" : {{"AWS" : "*"}},
      "Action":"SQS:SendMessage",
      "Resource": "${aws_sqs_queue.ml-user.arn}",
      "Condition":{
        "ArnEquals":{
          "aws:SourceArn": "${aws_sns_topic.ml-user.arn}"
        }
      }
    }
  ]
}
POLICY
}
