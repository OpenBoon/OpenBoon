from __future__ import print_function

import os
import re

import backoff

from pathlib2 import Path

from google.cloud import automl_v1beta1 as automl
from google.api_core.exceptions import ResourceExhausted

from zmlp.analysis import Argument, AssetBuilder, Generator


class GoogleApiProcessorMixin(object):
    def __init__(self):
        super(GoogleApiProcessorMixin, self).__init__()
        self.add_arg(Argument('gcp_credentials_path', 'string', default=None))

    def initialize_gcp_client(self, client_class):
        """Initiliazes a GC"""
        if self.arg_value('gcp_credentials_path'):
            credential_path = Path(self.arg_value('gcp_credentials_path')).expanduser().resolve()
            if not os.path.isfile(str(credential_path)):
                raise Exception(u"Can't find credentials file {}".format(credential_path))
            return client_class.from_service_account_file(str(credential_path))
        else:
            return client_class()


class GoogleApiDocumentProcessor(AssetBuilder, GoogleApiProcessorMixin):
    pass


class GoogleApiGenerator(GoogleApiProcessorMixin, Generator):
    pass


class AutoMLModelProcessor(GoogleApiDocumentProcessor):
    """Use a pre-trained Google AutoML model to label and score assets."""

    tool_tips = {
        'project_id': 'The project ID for the AutoML model (e.g. "zorroa-autoedl")',
        'location_id': 'The region ID for the AutoML model (e.g. "us-central1")',
        'model_id': '(Optional) The model ID for the AutoML model (e.g. "ICN1653624923981482691") '
                    'If this parameter is omitted, the most recently created model will be used.',
        'label_map': '(Optional) A mapping of from AutoML labels to ZVI labels '
                     '(e.g. {"automl_label_1": "zvi_label_1", ...} )',
    }

    def __init__(self):
        super(AutoMLModelProcessor, self).__init__()
        self.add_arg(Argument("project_id", "string", required=True,
                              toolTip=AutoMLModelProcessor.tool_tips['project_id']))
        self.add_arg(Argument("location_id", "string", required=True,
                              toolTip=AutoMLModelProcessor.tool_tips['location_id']))
        self.add_arg(Argument("model_id", "string",
                              toolTip=AutoMLModelProcessor.tool_tips['model_id']))
        self.add_arg(Argument("label_map", "dict",
                              toolTip=AutoMLModelProcessor.tool_tips['label_map']))

        self.prediction_client = None

    def init(self):
        super(AutoMLModelProcessor, self).init()
        self.project_id = self.arg_value('project_id')
        self.location_id = self.arg_value('location_id')
        self.model_id = self.arg_value('model_id')
        self.label_map = self.arg_value('label_map')

        self.prediction_client = self.initialize_gcp_client(automl.PredictionServiceClient)

        self.model_client = self.initialize_gcp_client(automl.AutoMlClient)
        self.model_parent = self.model_client.location_path(self.project_id, self.location_id)

        if self.model_id and len(self.model_id) > 0:
            self.model_path = 'projects/{}/locations/{}/models/{}'.format(self.project_id,
                                                                          self.location_id,
                                                                          self.model_id)
        else:
            self.model_path = self._get_latest_model_path()

        self.namespace = self._get_namespace()
        # self.logger.debug('Namespace: {}'.format(self.namespace))

    def _get_latest_model_path(self):
        latest_timestamp = 0
        latest_model_path = None
        for element in self.model_client.list_models(self.model_parent):
            if element.update_time.seconds > latest_timestamp:
                latest_timestamp = element.create_time.seconds
                latest_model_path = element.name
        return latest_model_path

    def _get_namespace(self):
        # First, get the human-friendly model name...
        model = self.model_client.get_model(self.model_path)
        model_name = model.display_name
        # ...except Google makes it unfriendly by attaching dates and versions, so remove those.
        match = re.match(r'(.*)_v\d{14}(_\d+)*$', model_name)
        if match:
            model_name = match.group(1)
        model_name = model_name.lower().replace(' ', '_')
        # Concatenate the processor and model name into the namespace
        return 'google.{}.{}'.format(self._proc_fieldname(), model_name)

    def _process(self, frame):
        asset = frame.asset
        self._announce(asset)
        outgoing_payload = self._create_payload(asset)
        if not outgoing_payload:
            self.logger.info("\tSkipping...")
            return

        result = self._predict(outgoing_payload)
        struct = {}
        for incoming_payload in enumerate(result.payload):
            automl_label_name = incoming_payload[1].display_name
            if automl_label_name in self.label_map:
                zvi_label_name = self.label_map[automl_label_name]
            else:
                zvi_label_name = automl_label_name.lower().replace(' ', '_')
            score = float(incoming_payload[1].classification.score)
            struct[zvi_label_name] = score

        self.logger.info("\tStoring into {}: {}".format(self.namespace, struct))
        asset.add_analysis(self.namespace, struct)

    def _announce(self, asset):
        raise NotImplementedError("_announce() must be overridden by subclasses of "
                                  "AutoMLModelProcessor")

    def _create_payload(self, asset):
        raise NotImplementedError("_create_payload() must be overridden by subclasses of "
                                  "AutoMLModelProcessor")

    def _proc_fieldname(self):
        raise NotImplementedError("_proc_fieldname() must be overridden by subclasses of "
                                  "AutoMLModelProcessor")

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _predict(self, payload):
        return self.prediction_client.predict(name=self.model_path,
                                              payload=payload,
                                              params={})
