import backoff
from google.api_core.exceptions import ResourceExhausted
from google.cloud import documentai_v1beta2 as documentai

from zmlpsdk import Argument, AssetProcessor, FileTypes
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.cloud import get_gcp_project_id
from .gcp_client import initialize_gcp_client


__all__ = [
    'CloudDocumentProcessor'
]


class CloudDocumentProcessor(AssetProcessor):
    """
    This base class is used for all Google Vision features.  Subclasses
    only have to implement the "detect(asset, image) method.
    """
    namespace = 'gcp-document-detection'
    file_types = FileTypes.images | FileTypes.documents

    def __init__(self):
        super(CloudDocumentProcessor, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.client = None

    def init(self):
        self.client = initialize_gcp_client(documentai.DocumentUnderstandingServiceClient)

    def process(self, frame):
        asset = frame.asset

        input_uri = asset.get_attr('source.path')
        if not input_uri.endswith('.pdf'):
            return

        document = self.parse_form_local(input_uri=input_uri)
        if not document:
            return

        analysis = LabelDetectionAnalysis()

        for page in document.pages:
            for form_field in page.form_fields:
                analysis.add_label_and_score(
                    label=self._get_text(document, form_field.field_name),
                    score=str(form_field.field_name.confidence),
                    field_value=self._get_text(document, form_field.field_value),
                    field_value_score=form_field.field_value.confidence
                )

        asset.add_analysis(self.namespace, analysis)

    def parse_form_local(self, input_uri=''):
        """ Process a single PDF document

        Args:
            input_uri (str): PDF filename (from source.path)

        Returns:
            (documentai.types.document.Document) processed response
        """
        gcs_source = documentai.types.GcsSource(uri=input_uri)

        # mime_type can be application/pdf, image/tiff, and image/gif, or application/json
        input_config = documentai.types.InputConfig(
            gcs_source=gcs_source,
            mime_type='application/pdf'
        )

        project_id = get_gcp_project_id()
        parent = 'projects/{}/locations/us'.format(project_id)
        request = documentai.types.ProcessDocumentRequest(
            parent=parent,
            input_config=input_config
        )

        return self._process_document(request)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _process_document(self, request):
        """

        Args:
            request: (documentai.types.ProcessDocumentRequest) request to process one document

        Returns:
            (documentai.types.document.Document) processed response
        """
        return self.client.process_document(request=request)

    @staticmethod
    def _get_text(document, field):
        """Doc AI identifies form fields by their offsets
        in document text. This function converts offsets
        to text snippets.

        Args:
            document: (~.document.Document) processed response
            field: (~.document.Document.Page.Layout) form field value

        Returns:
            (str) document response as text
        """
        response = ''
        # If a text segment spans several lines, it will be stored in different text segments.
        for segment in field.text_anchor.text_segments:
            start_index = segment.start_index
            end_index = segment.end_index
            response += document.text[start_index:end_index]
        return response
