import os
import json


def mock_data(name):
    return os.path.join(os.path.dirname(__file__), "", "mock-data", name)


class MockAwsCloudResources:
    pass


class MockSNSClient:
    def create_topic(self, Name=None):
        return {'TopicArn': 'abc123'}

    def subscribe(self, TopicArn=None, Protocol=None, Endpoint=None):
        return self

    def delete_topic(self, TopicArn=None):
        return self


class MockSQSClient:
    def create_queue(self, QueueName=None):
        return self

    def get_queue_url(self, QueueName=None):
        return {'QueueUrl': 'aws.com'}

    def get_queue_attributes(self, QueueUrl=None, AttributeNames=None):
        return {'Attributes': {'QueueArn': 'def456'}}

    def set_queue_attributes(self, QueueUrl=None, Attributes=None):
        return self

    def receive_message(self, QueueUrl=None, MessageAttributeNames=None, MaxNumberOfMessages=None):
        json_file = mock_data("sqs_response.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def delete_message(self, QueueUrl=None, ReceiptHandle=None):
        return self

    def delete_queue(self, QueueUrl=None):
        return self


class MockRekClient:

    def start_label_detection(self, Video=None, NotificationChannel=None):
        json_file = mock_data("label_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_text_detection(self, Video=None, NotificationChannel=None):
        json_file = mock_data("text_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_face_detection(self, Video=None, NotificationChannel=None):
        json_file = mock_data("face_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_content_moderation(self, Video=None, NotificationChannel=None):
        json_file = mock_data("nsfw_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_celebrity_recognition(self, Video=None, NotificationChannel=None):
        json_file = mock_data("celeb_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_person_tracking(self, Video=None, NotificationChannel=None):
        json_file = mock_data("person_tracking.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_segment_detection(self, Video=None, NotificationChannel=None, SegmentTypes=None,
                                Filters=None):
        json_file = mock_data("segment_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_label_detection(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = mock_data("label_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_text_detection(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = mock_data("text_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_face_detection(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = mock_data("face_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_content_moderation(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = mock_data("nsfw_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_celebrity_recognition(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = mock_data("celeb_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_person_tracking(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = mock_data("person_tracking_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_segment_detection(self, JobId=None, MaxResults=None, NextToken=None):
        json_file = mock_data("segment_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)


class MockS3Client:
    def __init__(self, *args, **kwargs):
        self.objects = MockS3Object()

    def upload_file(self, *args, **kwargs):
        return self

    def delete_object(self, **kwargs):
        return self


class MockS3Object:
    def __init__(self, *args, **kwargs):
        pass

    def delete(self, **kwargs):
        return self


def mock_clients(service):
    if service == 'sns':
        return MockSNSClient()
    elif service == 'sqs':
        return MockSQSClient()
    else:
        return None
