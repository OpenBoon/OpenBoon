

class FileStorageApp:
    """
    Methods for manipulating File Storage.
    """

    def __init__(self, app):
        self.app = app

    def get_cloud_location(self, entity_type, entity_id, category, name):
        """
        Provide the location of a file in cloud
        :param entity_type: Entity type like MODELS, ASSETS..
        :param entity_id: id of the entity
        :param category: file category stored in cloud
        :param name: file name
        :return: Dictionary containing uri, mediaType
        """
        url = f"/api/v3/files/_locate/{entity_type}/{entity_id}/{category}/{name}"
        return self.app.client.get(url)
