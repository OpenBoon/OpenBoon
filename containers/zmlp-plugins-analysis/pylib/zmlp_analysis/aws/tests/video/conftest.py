import os
import json


class MockRekClient:

    def start_label_detection(self, Video=None, NotificationChannel=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "label_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_text_detection(self, Video=None, NotificationChannel=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "text_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_face_detection(self, Video=None, NotificationChannel=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "face_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_content_moderation(self, Video=None, NotificationChannel=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "nsfw_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_celebrity_recognition(self, Video=None, NotificationChannel=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "celeb_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_person_tracking(self, Video=None, NotificationChannel=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "person_tracking.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def start_segment_detection(self, Video=None, NotificationChannel=None, SegmentTypes=None,
                                Filters=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "segment_detection.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_label_detection(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "label_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_text_detection(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "text_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_face_detection(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "face_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_content_moderation(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "nsfw_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_celebrity_recognition(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "celeb_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_person_tracking(self, JobId=None, MaxResults=None, NextToken=None, SortBy=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "person_tracking_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def get_segment_detection(self, JobId=None, MaxResults=None, NextToken=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "segment_detection_results.json")
        with open(json_file, "r") as fp:
            return json.load(fp)

    def create_project(self, ProjectName=None):
        return {'ProjectArn': 'testArn'}

    def start_project_version(self, ProjectVersionArn=None, MinInferenceUnits=None):
        return {'Status': 'STARTING'}

    def stop_project_version(self, ProjectVersionArn=None):
        return {'Status': 'STOPPED'}

    def describe_project_versions(self, ProjectArn=None, VersionNames=None):
        return {
            'ProjectVersionDescriptions': [{
                'Status': 'RUNNING',
                'StatusMessage': 'Ready to run.'
            }]
        }

    def create_project_version(self, ProjectArn=None, VersionName=None, OutputConfig=None,
                               TrainingData=None, TestingData=None):
        return {'ProjectVersionArn': 'test_projectVersionArn'}

    def get_waiter(self, waiter_name=None):
        return self

    def wait(self, ProjectArn=None, VersionNames=None):
        return self
