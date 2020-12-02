
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
