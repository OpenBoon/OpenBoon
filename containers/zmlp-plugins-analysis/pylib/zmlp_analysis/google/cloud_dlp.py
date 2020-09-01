import cv2

import google.cloud.dlp

from zmlpsdk import file_storage, Argument, AssetProcessor, FileTypes
from zmlpsdk.proxy import get_proxy_level_path, calculate_normalized_bbox
from zmlpsdk.analysis import LabelDetectionAnalysis, Prediction
from zmlpsdk.cloud import get_gcp_project_id
from .gcp_client import initialize_gcp_client

__all__ = [
    'CloudDLPDetectEntities'
]


class CloudDLPDetectEntities(AssetProcessor):
    """
    This base class is used for all Google Vision features.  Subclasses
    only have to implement the "detect(asset, image) method.
    """

    file_types = FileTypes.images | FileTypes.documents

    def __init__(self):
        super(CloudDLPDetectEntities, self).__init__()
        self.dlp_annotator = None
        self.add_arg(Argument('debug', 'bool', default=False))
        self.proxy_level = 3

    def init(self):
        self.dlp_annotator = initialize_gcp_client(google.cloud.dlp_v2.DlpServiceClient)

    def process(self, frame):
        asset = frame.asset

        info_types_list = ["PERSON_NAME", "STREET_ADDRESS", "DATE"]
        info_types = [{"name": info_type} for info_type in info_types_list]

        inspect_config = {
            "info_types": info_types,
            "custom_info_types": [],
            "min_likelihood": google.cloud.dlp_v2.Likelihood.LIKELIHOOD_UNSPECIFIED,
            "include_quote": True,
            "limits": {"max_findings_per_request": 0},
        }

        pid = get_gcp_project_id()
        parent = f"projects/{pid}"

        p_path = self.get_proxy_image(frame.asset)

        img = cv2.imread(p_path)
        item = {"byte_item": {"type": 1, "data": cv2.imencode('.jpg', img)[1].tobytes()}}

        rsp = self.dlp_annotator.inspect_content(
            request={"parent": parent, "inspect_config": inspect_config, "item": item}
        )

        findings = rsp.result.findings
        if not findings:
            return

        analysis_dict = {}
        for f in findings:
            # There are multiple bounding boxes per finding, potentially.
            # Here we find the bounding box of all those bboxes.
            xmin = 10000
            ymin = 10000
            xmax = 0
            ymax = 0
            for bbox in f.location.content_locations[0].image_location.bounding_boxes:
                x1 = bbox.left
                y1 = bbox.top
                x2 = x1 + bbox.width
                y2 = y1 + bbox.height

                if xmin > x1:
                    xmin = x1
                if ymin > y1:
                    ymin = y1
                if xmax < x2:
                    xmax = x2
                if ymax < y2:
                    ymax = y2

            rect = calculate_normalized_bbox(img.shape[1], img.shape[0],
                                             [xmin, ymin, xmax, ymax])

            if f.info_type.name not in analysis_dict:
                analysis_dict[f.info_type.name] = LabelDetectionAnalysis()

            analysis_dict[f.info_type.name].add_prediction(
                Prediction(
                    f.quote,
                    f.likelihood / 5.0,
                    bbox=rect
                )
            )

        for info_type in analysis_dict:
            asset.add_analysis('gcp-dlp-' + info_type.lower().replace('_', '-'),
                               analysis_dict[info_type])

    def get_proxy_image(self, asset):
        """
        Choose a proper proxy image effort OCR.

        Args:
            asset (Asset): The asset to look at.

        Returns:
            StoredFile: A StoredFile instance.
        """
        ocr_proxy = asset.get_files(category='ocr-proxy')
        if ocr_proxy:
            self.logger.info("OCR proxy detected")
            return file_storage.localize_file(ocr_proxy[0])
        else:
            return get_proxy_level_path(asset, 3)
