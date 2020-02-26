import re

import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import automl_v1beta1 as automl

from zmlpsdk import Argument, AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path


class AutoMLModelProcessor(AssetProcessor):
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


class AutoMLNLPModelProcessor(AutoMLModelProcessor):
    tool_tips = {
        'src_field': 'The metadata field that contains the data to evaluate with AutoML '
                     '(e.g. "analysis.google.documentTextDetection.content")',
        'collapse_multipage': 'True if you want to collapse all document pages into a single score,'
                              ' False if you want a score per page',
        'ignore_pages': 'List of pages in a document to ignore. Only used when collapse_multipage '
                        'is True. (e.g. "[1, 4]")'
    }

    def __init__(self):
        super(AutoMLNLPModelProcessor, self).__init__()
        self.add_arg(Argument("src_field", "string", required=True,
                              toolTip=AutoMLNLPModelProcessor.tool_tips['src_field']))
        self.add_arg(Argument("collapse_multipage", "bool", default=False,
                              toolTip=AutoMLNLPModelProcessor.tool_tips['collapse_multipage']))
        self.add_arg(Argument("ignore_pages", "list", default=[],
                              toolTip=AutoMLNLPModelProcessor.tool_tips['ignore_pages']))

    def init(self):
        super(AutoMLNLPModelProcessor, self).init()
        self.src_field = self.arg_value('src_field')
        self.collapse_multipage = self.arg_value('collapse_multipage')
        self.ignore_pages = self.arg_value('ignore_pages')

    def _announce(self, asset):
        self.logger.info("AutoMLNLPModelProcessor for asset {}".format(asset.id))

    def _proc_fieldname(self):
        return 'automl_nlp'

    def _concatenate_pages(self, asset):
        # Get all the child pages of this document and concatenate their contents
        # in order. Get the contents using self.src_field, and ignore any pages
        # mentioned in self.ignore_pages.

        page_count = int(asset.get_attr('media.pages'))
        # self.logger.debug("\tPage count: {}".format(page_count))
        pages = [''] * (page_count + 1)

        #
        # TODO: a ZMLP search instead.
        # archivist.AssetSearch().term_filter("media.clip.parent", asset.id)
        #
        search = []
        for child in search:
            # self.logger.debug("\t\tChild ID: {}".format(child.id))
            page_num = int(child.get_attr("media.clip.start"))
            # self.logger.debug("\t\tPage num {}".format(page_num))
            val = child.get_attr(self.src_field)
            # self.logger.debug("\t\tField {} -> {}".format(self.src_field, val))

            # WARNING!!! We're forcing this to be a string, because if "src_field"
            # refers to a field that isn't a string, concatenation will fail. But
            # in cases like Commerzbank where the model was trained on UTF-8 data,
            # this might make things _worse_. We might need to change this to be
            # something like:  pages[page_num] = val.encode('utf-8')
            pages[page_num] = str(val)

        for i in range(len(pages)):
            if i in self.ignore_pages:
                # self.logger.debug("\t\tIgnoring page {}".format(i))
                pages[i] = ''

        return "\n".join(pages)

    def _create_payload(self, asset):
        is_full_document = not asset.get_attr("media.clip.parent")
        # self.logger.debug("\tfull doc: {}".format(is_full_document))

        if self.collapse_multipage:
            if is_full_document:
                content = self._concatenate_pages(asset)
            else:
                # If we're collapsing we only care about the parent at this point.
                # So if we're on a child, bail out.
                return
        else:
            content = asset.get_attr(self.src_field)

        self.logger.info("\tContent is {} bytes".format(len(content)))
        # self.logger.debug("\tContent: {}".format(content))
        return {
            "text_snippet": {
                "content": content,
                "mime_type": "text/plain"
            }
        }


class AutoMLVisionModelProcessor(AutoMLModelProcessor):
    def _announce(self, asset):
        self.logger.info("AutoMLVisionModelProcessor for asset {}".format(asset.id))

    def _proc_fieldname(self):
        return 'automl_vision'

    def _create_payload(self, asset):
        file_path = get_proxy_level_path(asset, 1)
        with open(file_path, 'rb') as fh:
            content = fh.read()

        self.logger.info("\tRead {} bytes from {}".format(len(content), file_path))
        # self.logger.debug("\tContent is: {}".format(str(content)))
        return {
            "image": {
                "image_bytes": content
            }
        }
