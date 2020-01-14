import cv2
import cvlib

from zmlpsdk import AssetProcessor
from zmlpsdk.proxy import get_proxy_level, store_element_proxy
from zmlp.elements import Element

BOX_COLOR = (0, 255, 255)


class ZmlpFaceDetectionProcessor(AssetProcessor):
    """
    Simple Face Detection processor
    """
    namespace = "zmlpFaceDetection"

    def __init__(self):
        super(ZmlpFaceDetectionProcessor, self).__init__()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level(asset, 3)
        img = cv2.imread(p_path)

        faces, confidences = cvlib.detect_face(img)

        if not faces:
            return

        # Points are messed up compared to face recognition.
        rects = []
        for face in faces:
            rects.append([int(face[2]), int(face[3]), int(face[0]), int(face[1])])

        # draw the face proxy
        face_proxy = store_element_proxy(asset,
                                         img,
                                         self.namespace,
                                         rects=rects,
                                         color=BOX_COLOR)

        # Create the elements.
        for rect, conf in zip(rects, confidences):
            element = Element('face', analysis=self.namespace,
                              rect=rect, score=float(conf), proxy=face_proxy)
            asset.add_element(element)
