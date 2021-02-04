import cv2

import google.cloud.dlp

from zmlpsdk import file_storage, Argument, AssetProcessor, FileTypes
from zmlpsdk.proxy import get_proxy_level_path, calculate_normalized_bbox
from zmlpsdk.analysis import LabelDetectionAnalysis, Prediction
from zmlpsdk.cloud import get_gcp_project_id
from .gcp_client import initialize_gcp_client

from nameparser import HumanName
import dateparser
from streetaddress import StreetAddressParser


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
            "min_likelihood": "LIKELIHOOD_UNSPECIFIED",
            "include_quote": True,
            "limits": {"max_findings_per_request": 0},
        }

        pid = get_gcp_project_id()
        parent = f"projects/{pid}"

        p_path = self.get_proxy_image(frame.asset)

        img = cv2.imread(p_path)
        item = {"byte_item": {"type": 1, "data": cv2.imencode('.jpg', img)[1].tobytes()}}

        rsp = self.dlp_annotator.inspect_content(parent=parent,
                                                 inspect_config=inspect_config,
                                                 item=item)

        findings = rsp.result.findings
        if not findings:
            return

        analysis_dict = {}
        for f in findings:

            # Sanitize value, skip if sanitized result is empty
            value = self.sanitize_entity(f.info_type.name, f.quote)
            if not value:
                continue

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
                    value,
                    f.likelihood / 4.0,
                    bbox=rect
                )
            )

        for info_type in analysis_dict:
            asset.add_analysis('gcp-dlp-' + info_type.lower().replace('_', '-'),
                               analysis_dict[info_type])

        # This stores the raw google result in case we need it later.
        file_storage.assets.store_blob(rsp._pb.SerializeToString(),
                                       asset,
                                       'gcp',
                                       'dlp.dat')

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

    def sanitize_entity(self, info_type, value):
        """
        Sanitize values coming from DLP.

        Args:
            info_type (str): the type of the entity to sanitize.
            value (str): the value of te entity.

        Returns:
            str: sanitized value, can be an empty string if entity was incomplete.
        """
        if info_type == 'PERSON_NAME':
            name = HumanName(value)
            name.capitalize(force=True)
            name.title = ''
            name.nickname = ''

            value = name.full_name

            if len(value.split(' ')) < 2:
                value = ''

        elif info_type == 'STREET_ADDRESS':
            value = value.title()
            addr_parser = StreetAddressParser()
            addr = addr_parser.parse(value)
            if not addr['house'] or not addr['street_name']:
                value = ''

            # Do some replacements in order to standardize addresses
            replace = {'.': '',
                       ',': ' ',
                       'Po ': 'P.O. ',
                       'Avenue': 'Ave',
                       'Street': 'St',
                       'Place': 'Pl',
                       'Lane': 'Ln',
                       'Road': 'Rd'
                       }

            for key in replace:
                value = value.replace(key, replace[key])

            # Remove double spaces
            value = ' '.join(value.split())

        elif info_type == 'DATE':
            parsers = [parser for parser in
                       dateparser.conf.settings.PARSERS if parser != 'relative-time']
            date = dateparser.parse(value,
                                    settings={'STRICT_PARSING': True, 'PARSERS': parsers})

            if date:
                value = date.strftime("%m/%d/%Y")
            else:
                value = ''

        return value
