
from ..entity import Project


class ProjectApp(object):

    def __init__(self, app):
        self.app = app

    def get_project(self):
        """
        Return the current API Key's assigned project.

        Returns:
            Project
        """
        return Project(self.app.client.get("/api/v1/project"))
