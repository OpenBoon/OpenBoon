from boonflow import ModelTrainer, FatalProcessorException
from boonsdk import ModelType
from .labels import AutomlLabelDetectionSession


class AutoMLModelTrainer(ModelTrainer):
    """Create Google AutoML Model"""

    def __init__(self):
        super(AutoMLModelTrainer, self).__init__()
        self.session = None

    def init(self):
        self.load_app_model()

    def train(self):
        # Check the type of model and use the correct session class.
        if self.app_model.type == ModelType.GCP_AUTOML_CLASSIFIER:
            session = AutomlLabelDetectionSession(self.app_model,
                                                  self.reactor,
                                                  self.arg_value('training_bucket'))
        else:
            raise FatalProcessorException(f'{self.app_model.type} is not supported. ')

        self.reactor.emit_status("Training AutoML model")
        session.train()
