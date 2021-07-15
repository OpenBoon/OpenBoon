from boonflow import AssetProcessor, Argument, FileTypes
from boonflow import file_storage


class CustomModelProcessor(AssetProcessor):
    """
    CustomModelProcessor is a base class for custom models.
    """
    file_types = FileTypes.all

    def __init__(self):
        super(CustomModelProcessor, self).__init__()
        self.add_arg(Argument('model_id', 'str', required=True, toolTip='The model Id'))
        self.add_arg(Argument('tag', 'str', required=False, toolTip='A optional model tag'))
        self.add_arg(Argument('min_score', 'float', required=False, default=0.1))
        self.app_model = None

    @property
    def min_score(self):
        return self.arg_value('min_score')

    def load_app_model(self):
        """
        Load the Boon AI model metadata.
        """
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))

    def get_model_path(self):
        """
        Return the file path to the custom model.

        Returns:
            str: The path to the unzipped model.
        """
        mid = self.arg_value("model_id")
        tag = self.arg_value('tag') or self.context.settings.get(f'{mid}:tag', None)
        if not tag:
            tag_order = ('approved', 'latest', 'model')
            tags = self.app.models.get_model_version_tags(mid)
            for _tag in tag_order:
                if _tag in tags:
                    tag = _tag
                    break
        if not tag:
            raise ValueError(f'The model {mid} has no tagged versions')

        return file_storage.models.install_model(self.app_model, tag)
