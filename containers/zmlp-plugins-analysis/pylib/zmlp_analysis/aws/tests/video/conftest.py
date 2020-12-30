import os
import json


class MockRekClient:

    def detect_custom_labels(self, Image=None, ProjectVersionArn=None):
        json_file = os.path.join(os.path.dirname(__file__), "..", "mock-data",
                                 "detect_custom_labels.json")
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
