package boonai.archivist.domain

interface Aws

class AwsTopicAndQueue(
    val queueArn: String,
    val queueUrl: String,
    val topicArn: String,

)
