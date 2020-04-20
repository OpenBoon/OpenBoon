
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

    def get_project_settings(self):
        """
        Return the current API Key's assigned project.

        Returns:
            Project
        """
        return self.app.client.get("/api/v1/project/_settings")

    def update_project_settings(self, settings):
        """
        Update the project's settings and return the new settings dict.

        Returns:
            dict
        """
        return self.app.client.put("/api/v1/project/_settings", settings)
