package com.zorroa.archivist.domain

interface Aws

class AwsTopicAndQueue(
    val queueArn: String,
    val queueUrl: String,
    val topicArn: String,

)