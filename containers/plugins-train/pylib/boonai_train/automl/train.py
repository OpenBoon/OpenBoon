from boonsdk import ModelType
from boonflow import Argument, AssetProcessor, FatalProcessorException
from .labels import AutomlLabelDetectionSession


class AutoMLModelTrainer(AssetProcessor):
    """Create Google AutoML Model"""

    def __init__(self):
        super(AutoMLModelTrainer, self).__init__()
        self.add_arg(Argument("model_id", "string", required=True, toolTip="The model Id"))

        self.app_model = None
        self.session = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))

    def process(self, frame):
        # Check the type of model and use the correct session class.
        if self.app_model.type == ModelType.GCP_LABEL_DETECTION:
            session = AutomlLabelDetectionSession(self.app_model, self.reactor)
        else:
            raise FatalProcessorException(f'{self.app_model.type} is not supported. ')

        self.reactor.emit_status("Training AutoML model")
        session.train()
