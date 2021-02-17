from boonsdk.util import as_id

from .index_app import IndexApp
from .project_app import ProjectApp

__all__ = [
    'BoonCzarApp',
    'from_app'
]


class BoonCzarApp(object):
    """
    Exposes the ZMLP administrator API.
    """
    def __init__(self, app):
        self.client = app.client
        self.indexes = IndexApp(app)
        self.projects = ProjectApp(app)

    def set_project(self, project):
        """
        Set the project the client is manipulating.

        Args:
            project (Project): The project or unique project id.

        """
        self.client.project_id = as_id(project)


def from_app(app):
    """
    Create and return an BoonCzarApp instance.

    Args:
        app (BoonApp): A BoonApp instance.

    Returns:
        BoonCzarApp: A  BoonCzarApp instance.
    """
    return BoonCzarApp(app)
