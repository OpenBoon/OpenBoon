from zmlp.entity import Project, ProjectTier

from ..entity.project import ProjectSize

__all__ = [
    'ProjectApp'
]


class ProjectApp(object):

    def __init__(self, app):
        self.app = app

    def get_project(self, pid):
        """
        Get a project by it's unique Id.

        Args:
            pid (str): The project Id.

        Returns:
            Project
        """
        return Project(self.app.client.get(f'/api/v1/projects/{pid}'))

    def create_project(self, name, tier=ProjectTier.ESSENTIALS, size=ProjectSize.SMALL, pid=None):
        """
        Create a project.

        Args:
            name (str): The name of the project.
            tier: (ProjectTier): The project tier.
            size (ProjectSize): Used to determine the default index settings.
            pid: (str): An optional project UUID, otherwise its randomly generated.

        Returns:
            Project: The newly created project.
        """
        body = {
            'name': name,
            'tier': tier.name,
            'size': size.name,
            'id': pid
        }
        return Project(self.app.client.post('/api/v1/projects', body))
