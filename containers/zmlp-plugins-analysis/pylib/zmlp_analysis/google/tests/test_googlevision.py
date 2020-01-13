
from google.cloud.vision import types
from mock import patch

from ..processors import CloudVisionProcessor
from zmlp import ZmlpClient
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_data, TestAsset
from zmlpsdk import Frame, ZmlpFatalProcessorException

patch_path = 'zmlp_analysis.google.processors.vision.ImageAnnotatorClient'

TOUCAN = zorroa_test_data("images/set01/toucan.jpg")
EIFFEL = zorroa_test_data("images/set08/eiffel_tower.jpg")
PUNCH = zorroa_test_data("images/set09/punch.jpg")
FACES = zorroa_test_data("images/set01/faces.jpg")
STREETSIGN = zorroa_test_data("images/set09/streetsign.jpg")
MANUAL = zorroa_test_data("images/set09/nvidia_manual_page.jpg")
TOOBIG = zorroa_test_data("images/set09/heckabig.jpg")

PROXY_FILE = {
    "name": "proxy_512x341.jpg",
    "category": "proxy",
    "mimetype": "image/jpeg",
    "assetId": "12345",
    "attrs": {
        "width": 512,
        "height": 341
    }
}


# simulate the gcp client that calls the google APIs
class MockImageAnnotatorClient:
    def __init__(self):
        pass

    def label_detection(self, image):
        mock_labels = [
            types.EntityAnnotation(description="Toucan",
                                   score=0.993764221668),
            types.EntityAnnotation(description="Beak",
                                   score=0.970523297787),
            types.EntityAnnotation(description="Bird",
                                   score=0.968425631523),
            types.EntityAnnotation(description="Close-up",
                                   score=0.893450856209),
            types.EntityAnnotation(description="Piciformes",
                                   score=0.801966965199),
            types.EntityAnnotation(description="Organism",
                                   score=0.767177462578),
            types.EntityAnnotation(description="Macro photography",
                                   score=0.766558766365),
            types.EntityAnnotation(description="Eye",
                                   score=0.758622765541),
            types.EntityAnnotation(description="Photography",
                                   score=0.624224960804),
            types.EntityAnnotation(description="Wildlife",
                                   score=0.576150417328)
        ]
        response = types.AnnotateImageResponse(label_annotations=mock_labels)
        return response

    def landmark_detection(self, image):
        mock_landmarks = [
            types.EntityAnnotation(description="Eiffel Tower",
                                   score=0.542771399021,
                                   locations=[
                                       types.LocationInfo(
                                           lat_lng=types.LatLng(
                                               latitude=48.858461,
                                               longitude=2.294351)
                                       )
                                   ]),
            types.EntityAnnotation(description="France Eiffel Hotel",
                                   score=0.166629374027,
                                   locations=[
                                       types.LocationInfo(lat_lng=types.LatLng(
                                           latitude=48.863783,
                                           longitude=2.290155)
                                       )
                                   ])
        ]
        res = types.AnnotateImageResponse(landmark_annotations=mock_landmarks)
        return res

    def safe_search_detection(self, image):
        mock_res = types.SafeSearchAnnotation(adult="VERY_UNLIKELY",
                                              spoof="UNLIKELY",
                                              medical="VERY_UNLIKELY",
                                              violence="VERY_UNLIKELY",
                                              racy="UNLIKELY")

        res = types.AnnotateImageResponse(safe_search_annotation=mock_res)
        return res

    def face_detection(self, image):
        mock_annotations = [
            types.FaceAnnotation(roll_angle=3.85948371887,
                                 pan_angle=-4.00120306015,
                                 tilt_angle=-5.03876304626,
                                 detection_confidence=0.996880471706,
                                 landmarking_confidence=0.705760359764,
                                 joy_likelihood="UNLIKELY",
                                 sorrow_likelihood="VERY_UNLIKELY",
                                 anger_likelihood="VERY_UNLIKELY",
                                 surprise_likelihood="VERY_UNLIKELY",
                                 under_exposed_likelihood="VERY_UNLIKELY",
                                 blurred_likelihood="VERY_UNLIKELY",
                                 headwear_likelihood="VERY_UNLIKELY"),
            types.FaceAnnotation(roll_angle=-0.48230355978,
                                 pan_angle=1.49939823151,
                                 tilt_angle=-10.4569005966,
                                 detection_confidence=0.973327457905,
                                 landmarking_confidence=0.679704546928,
                                 joy_likelihood="UNLIKELY",
                                 sorrow_likelihood="VERY_UNLIKELY",
                                 anger_likelihood="VERY_UNLIKELY",
                                 surprise_likelihood="VERY_UNLIKELY",
                                 under_exposed_likelihood="VERY_UNLIKELY",
                                 blurred_likelihood="VERY_UNLIKELY",
                                 headwear_likelihood="VERY_UNLIKELY")
        ]
        res = types.AnnotateImageResponse(face_annotations=mock_annotations)
        return res

    def text_detection(self, image):
        img_txt = u'PASEO TAMAYO\nNIRVANA 6400 N\nNO OUTLET\u2192\nSTOP\n'
        text_annotation = types.TextAnnotation(pages=None,
                                               text=img_txt)
        res = types.AnnotateImageResponse(full_text_annotation=text_annotation)
        return res

    def document_text_detection(self, image):
        manual_text = u'Notice\nThe information provided in this ' \
                      u'specification ' \
                      u'is believed to be accurate and ' \
                      u'reliable as of the date provided. ' \
                      u'However, NVIDIA\nCorporation ("NVIDIA") ' \
                      u'does not give ' \
                      u'any representations or warranties, ' \
                      u'expressed or implied, as to the ' \
                      u'accuracy or completeness ' \
                      u'of\nsuch information. NVIDIA shall ' \
                      u'have no liability for the consequences ' \
                      u'or use of such ' \
                      u'information or for any infringement ' \
                      u'of patents\nor other rights of third ' \
                      u'parties that may' \
                      u' result from its use. This publication ' \
                      u'supersedes and replaces all other ' \
                      u'specifications for ' \
                      u'the\nproduct that may have been ' \
                      u'previously supplied.\nNVIDIA reserves ' \
                      u'the right to make ' \
                      u'corrections, modifications, ' \
                      u'enhancements, improvements, and ' \
                      u'other changes to this ' \
                      u'specification,\nat any time and/or ' \
                      u'to discontinue any product or ' \
                      u'service without notice. ' \
                      u'Customer should obtain the latest ' \
                      u'relevant specification\nbefore placing ' \
                      u'orders and should ' \
                      u'verify that such information is current ' \
                      u'and complete.\nNVIDIA products are sold ' \
                      u'subject to ' \
                      u'the NVIDIA standard terms and conditions ' \
                      u'of sale supplied at the time of ' \
                      u'order\nacknowledgement, unless otherwise ' \
                      u'agreed in an individual sales agreement ' \
                      u'signed by ' \
                      u'authorized representatives of NVIDIA ' \
                      u'and\ncustomer. NVIDIA hereby expressly ' \
                      u'objects to ' \
                      u'applying any customer general terms ' \
                      u'and conditions with regard to the ' \
                      u'purchase ' \
                      u'of\nthe NVIDIA ' \
                      u'product referenced in this ' \
                      u'specification.\nNVIDIA products ' \
                      u'are not designed, authorized or ' \
                      u'warranted to be suitable for use ' \
                      u'in medical, military, aircraft, ' \
                      u'space or life ' \
                      u'support\nequipment, nor in ' \
                      u'applications where failure or ' \
                      u'malfunction of the NVIDIA ' \
                      u'product can reasonably be expected ' \
                      u'to result in personal\ninjury, ' \
                      u'death or property or ' \
                      u'environmental damage. NVIDIA accepts ' \
                      u'no liability for inclusion ' \
                      u'and/or use of NVIDIA products ' \
                      u'in such\nequipment or applications ' \
                      u'and therefore such inclusion ' \
                      u'and/or use is at customer\'s ' \
                      u'own risk.\nNVIDIA makes no ' \
                      u'representation or warranty that ' \
                      u'products based on ' \
                      u'these specifications will be ' \
                      u'suitable for any specified use ' \
                      u'without\nfurther ' \
                      u'testing or modification. Testing ' \
                      u'of all parameters of each product ' \
                      u'is not necessarily ' \
                      u'performed by NVIDIA. It is ' \
                      u'customer\'s\nsole responsibility ' \
                      u'to ensure the product is' \
                      u' suitable and fit for the ' \
                      u'application planned by customer ' \
                      u'and to do the necessary ' \
                      u'testing\nfor the application ' \
                      u'in order to avoid a default ' \
                      u'of the application or the product. ' \
                      u'Weaknesses in customer\'s ' \
                      u'product designs may ' \
                      u'affect\nthe quality and ' \
                      u'reliability of the ' \
                      u'NVIDIA product and may result in ' \
                      u'additional or different ' \
                      u'conditions and/or requirements ' \
                      u'beyond\nthose contained ' \
                      u'in this specification. ' \
                      u'NVIDIA does not accept ' \
                      u'any liability related ' \
                      u'to any default, damage, ' \
                      u'costs or problem ' \
                      u'which\nmay be based ' \
                      u'on or attributable ' \
                      u'to: ' \
                      u'(i) the use of ' \
                      u'the NVIDIA product ' \
                      u'in any manner that ' \
                      u'is contrary to ' \
                      u'this specification, or ' \
                      u'(ii)\ncustomer product ' \
                      u'designs.\nNo license, ' \
                      u'either expressed or ' \
                      u'implied, is granted under ' \
                      u'any NVIDIA patent ' \
                      u'right, copyright, or other NVIDIA ' \
                      u'intellectual property\nright under this ' \
                      u'specification. ' \
                      u'Information published ' \
                      u'by NVIDIA regarding ' \
                      u'third-party products ' \
                      u'or services' \
                      u' does not constitute ' \
                      u'a\nlicense from NVIDIA ' \
                      u'to use such ' \
                      u'products or services ' \
                      u'or a warranty or' \
                      u' endorsement thereof. ' \
                      u'Use of such information ' \
                      u'may require a\nlicense ' \
                      u'from a third party ' \
                      u'under the patents or ' \
                      u'other intellectual property rights of the ' \
                      u'third party, or a ' \
                      u'license from ' \
                      u'NVIDIA under ' \
                      u'the\npatents or ' \
                      u'other intellectual ' \
                      u'property rights of ' \
                      u'NVIDIA. Reproduction of ' \
                      u'information in this ' \
                      u'specification is ' \
                      u' only if\nreproduction ' \
                      u'is approved by NVIDIA' \
                      u' in writing, is reproduced ' \
                      u'without alteration, ' \
                      u'and is accompanied by all ' \
                      u'associated ' \
                      u'conditions,\nlimitations, ' \
                      u'and notices.\nALL ' \
                      u'NVIDIA DESIGN SPECIFICATIONS, ' \
                      u'REFERENCE BOARDS, FILES, ' \
                      u'DRAWINGS, DIAGNOSTICS, ' \
                      u'LISTS, AND OTHER\nDOCUMENTS ' \
                      u'(TOGETHER AND SEPARATELY, ' \
                      u'"MATERIALS") ARE BEING ' \
                      u'PROVIDED "AS IS." NVIDIA ' \
                      u'MAKES ' \
                      u'NO WARRANTIES,\nEXPRESSED, ' \
                      u'IMPLIED, STATUTORY, OR ' \
                      u'OTHERWISE WITH RESPECT ' \
                      u'TO THE MATERIALS, ' \
                      u'AND EXPRESSLY DISCLAIMS ' \
                      u'ALL\nIMPpLIED WARRANTIES ' \
                      u'OF NONINFRINGEMENT, MERCHANTABILITY, AND ' \
                      u'FITNESS FOR A PARTICULAR ' \
                      u'PURPOSE.\nNotwithstanding ' \
                      u'any damages that customer ' \
                      u'might incur ' \
                      u'for any reason whatsoever, ' \
                      u'NVIDIA\'s aggregate and ' \
                      u'cumulative liability\ntowards ' \
                      u'customer for ' \
                      u'the products described herein ' \
                      u'shall be limited in accordance ' \
                      u'with the NVIDIA terms and ' \
                      u'conditions of sale for\nthe ' \
                      u'product.\nVESA ' \
                      u'DisplayPort\nDisplayPort ' \
                      u'and DisplayPort' \
                      u' Compliance Logo, DisplayPort ' \
                      u'Compliance Logo for ' \
                      u'Dual-mode Sources, and DisplayPort ' \
                      u'Compliance\nLogo for ' \
                      u'Active Cables are ' \
                      u'trademarks owned ' \
                      u'by the Video Electronics Standards' \
                      u' Association in the United States and ' \
                      u'other\ncountries.\nHDMI\nHDMI, the HDMI logo, and' \
                      u' High-Definition ' \
                      u'Multimedia Interface ' \
                      u'are trademarks or ' \
                      u'registered ' \
                      u'trademarks of HDMI ' \
                      u'Licensing LLC.\nOpenCL\nOpenCL ' \
                      u'is a trademark of Apple Inc. ' \
                      u'used under license to the' \
                      u' Khronos Group ' \
                      u'Inc.\nTrademarks\nNVIDIA, ' \
                      u'the NVIDIA logo, CUDA, ' \
                      u'Game Works, GameStream, ' \
                      u'GeForce, GeForce GTX, GeForce ' \
                      u'Experience, Giga Thread, ' \
                      u'GPU-\nBOOST, G-Sync, Lumenex, ' \
                      u'PureVideo, PhysX, ShadowPlay, ' \
                      u'and TXAA are trademarks or ' \
                      u'registered trademarks ' \
                      u'of NVIDIA\nCorporation. ' \
                      u'Other company product names ' \
                      u'may be trademarks of the ' \
                      u'respective' \
                      u' companies with which they ' \
                      u'are associated.\nCopyright\n\xa9 ' \
                      u'2016 NVIDIA Corporation. ' \
                      u'All rights reserved.\nNVIDIA\n'

        text_annotation = types.TextAnnotation(pages=None, text=manual_text)
        res = types.AnnotateImageResponse(full_text_annotation=text_annotation)
        return res


class GoogleVisionUnitTestCase(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'upload_file')
    @patch(patch_path, side_effect=MockImageAnnotatorClient)
    def test_too_big(self, mock_image_annotator, upload_patch):
        # initialize doomed asset and initialize the processor
        upload_patch.return_value = PROXY_FILE
        asset = TestAsset(TOOBIG)
        frame = Frame(asset)
        store_asset_proxy(asset, TOOBIG, (200, 200))
        processor = self.init_processor(CloudVisionProcessor(), {})

        # see if the processor throws an exception for the image being too big
        self.assertRaises(ZmlpFatalProcessorException, processor.process,
                          frame)

    @patch.object(ZmlpClient, 'upload_file')
    @patch(patch_path, side_effect=MockImageAnnotatorClient)
    def test_detect_label(self, mock_image_annotator, upload_patch):
        # initialize asset and processor
        upload_patch.return_value = PROXY_FILE
        asset = TestAsset(TOUCAN)
        frame = Frame(asset)
        store_asset_proxy(asset, TOUCAN, (200, 200))

        processor = self.init_processor(CloudVisionProcessor(), {
            "detect_image_text": False,
            "detect_document_text": False,
            "detect_landmarks": False,
            "detect_explicit": False,
            "detect_faces": False,
            "detect_web_entities": False,
            "detect_logos": False,
            "detect_objects": False,
            "detect_labels": True
        })

        # run processor with declared frame and assert asset attributes
        processor.process(frame)
        keyword_pth = "analysis.google.visionLabelDetection.keywords"

        self.assertIn(u'Close-up', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Eye', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Photography', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Macro photography', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Wildlife', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Toucan', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Piciformes', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Beak', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Organism', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Bird', frame.asset.get_attr(keyword_pth))

    @patch.object(ZmlpClient, 'upload_file')
    @patch(patch_path, side_effect=MockImageAnnotatorClient)
    def test_detect_label_with_debug(self, mock_image_annotator, upload_patch):
        # initialize asset and processor
        upload_patch.return_value = PROXY_FILE
        asset = TestAsset(TOUCAN)
        frame = Frame(asset)
        store_asset_proxy(asset, TOUCAN, (200, 200))
        processor = self.init_processor(CloudVisionProcessor(), {
            "detect_image_text": False,
            "detect_document_text": False,
            "detect_landmarks": False,
            "detect_explicit": False,
            "detect_faces": False,
            "detect_labels": True,
            "detect_web_entities": False,
            "detect_logos": False,
            "detect_objects": False,
            "debug": True
        })

        # run processor with declared frame and assert normal and debug values
        processor.process(frame)

        # assert asset's normal visionLabelDetection data
        keyword_pth = "analysis.google.visionLabelDetection.keywords"
        self.assertIn(u'Close-up', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Eye', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Photography', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Macro photography', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Wildlife', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Toucan', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Piciformes', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Beak', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Organism', frame.asset.get_attr(keyword_pth))
        self.assertIn(u'Bird', frame.asset.get_attr(keyword_pth))
        # assert debug values
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred0"), u'Toucan')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred1"), u'Beak')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred2"), u'Bird')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred3"), u'Close-up')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred4"), u'Piciformes')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred5"), u'Organism')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred6"),
            u'Macro photography')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred7"), u'Eye')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred8"), u'Photography')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.pred9"), u'Wildlife')
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.type"), 'GCLabelDetection')
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob0"),
            0.9937642216682434, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob1"),
            0.9705232977867126, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob2"),
            0.9684256315231323, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob3"),
            0.8934508562088013, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob4"),
            0.8019669651985168, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob5"),
            0.7671774625778198, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob6"),
            0.7665587663650513, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob7"),
            0.7586227655410767, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob8"),
            0.6242249608039856, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.prob9"),
            0.5761504173278809, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.visionLabelDetection.scores"),
            0.9937642216682434, places=5)

    @patch.object(ZmlpClient, 'upload_file')
    @patch(patch_path, side_effect=MockImageAnnotatorClient)
    def test_detect_landmark(self, mock_image_annotator, upload_patch):
        # initialize asset and processor
        upload_patch.return_value = PROXY_FILE
        asset = TestAsset(EIFFEL)
        frame = Frame(asset)
        store_asset_proxy(asset, EIFFEL, (200, 200))
        processor = self.init_processor(CloudVisionProcessor(), {
            "detect_image_text": False,
            "detect_document_text": False,
            "detect_landmarks": True,
            "detect_explicit": False,
            "detect_faces": False,
            "detect_labels": False,
            "detect_logos": False,
            "detect_objects": False,
            "detect_web_entities": False
        })

        # run processor with declared frame and assert asset attributes
        processor.process(frame)
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.landmarkDetection.keywords"),
            u'Eiffel Tower')
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.landmarkDetection.point")[0],
            2.294351, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.landmarkDetection.point")[1],
            48.858461, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(
            "analysis.google.landmarkDetection.score"),
            0.5427713990211487, places=5)

    @patch.object(ZmlpClient, 'upload_file')
    @patch(patch_path, side_effect=MockImageAnnotatorClient)
    def test_detect_explicit(self, mock_image_annotator, upload_patch):
        # initialize asset and processor
        upload_patch.return_value = PROXY_FILE
        asset = TestAsset(PUNCH)
        frame = Frame(asset)
        store_asset_proxy(asset, PUNCH, (200, 200))
        processor = self.init_processor(CloudVisionProcessor(), {
            "detect_image_text": False,
            "detect_document_text": False,
            "detect_landmarks": False,
            "detect_explicit": True,
            "detect_faces": False,
            "detect_labels": False,
            "detect_logos": False,
            "detect_objects": False,
            "detect_web_entities": False
        })

        # run processor with declared asset and assert asset attributes
        processor.process(frame)
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.explicit.adult"), 0.0)
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.explicit.medical"), 0.0)
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.explicit.racy"), 0.25)
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.explicit.spoof"), 0.25)
        self.assertEqual(frame.asset.get_attr(
            "analysis.google.explicit.violence"), 0.0)

    @patch.object(ZmlpClient, 'upload_file')
    @patch(patch_path, side_effect=MockImageAnnotatorClient)
    def test_detect_faces(self, mock_image_annotator, upload_patch):
        # initialize the asset and processor
        upload_patch.return_value = PROXY_FILE
        asset = TestAsset(FACES)
        frame = Frame(asset)
        store_asset_proxy(asset, FACES, (200, 200))
        processor = self.init_processor(CloudVisionProcessor(), {
            "detect_image_text": False,
            "detect_document_text": False,
            "detect_landmarks": False,
            "detect_explicit": False,
            "detect_faces": True,
            "detect_labels": False,
            "detect_logos": False,
            "detect_objects": False,
            "detect_web_entities": False
        })

        # run the processor with declared frame and assert asset attributes
        processor.process(frame)
        anger_attr = "analysis.google.faceDetection.anger_likelihood"
        blurr_attr = "analysis.google.faceDetection.blurred_likelihood"
        detect_attr = "analysis.google.faceDetection.detection_confidence"
        head_attr = "analysis.google.faceDetection.headwear_likelihood"
        joy_attr = "analysis.google.faceDetection.joy_likelihood"
        keywrds_attr = "analysis.google.faceDetection.keywords"
        pan_attr = "analysis.google.faceDetection.pan_angle"
        roll_attr = "analysis.google.faceDetection.roll_angle"
        sorrow_attr = "analysis.google.faceDetection.sorrow_likelihood"
        surprise_attr = "analysis.google.faceDetection.surprise_likelihood"
        face_attr = "analysis.google.faceDetection.tilt_angle"
        ex_attr = "analysis.google.faceDetection.under_exposed_likelihood"

        self.assertEqual(frame.asset.get_attr(anger_attr), 1)
        self.assertEqual(frame.asset.get_attr(blurr_attr), 1)
        self.assertAlmostEqual(frame.asset.get_attr(detect_attr),
                               0.9968804717063904, places=5)
        self.assertEqual(frame.asset.get_attr(head_attr), 1)
        self.assertEqual(frame.asset.get_attr(joy_attr), 2)
        self.assertEqual(frame.asset.get_attr(keywrds_attr), [])
        self.assertAlmostEqual(frame.asset.get_attr(pan_attr),
                               -4.0012030601501465, places=5)
        self.assertAlmostEqual(frame.asset.get_attr(roll_attr),
                               3.8594837188720703, places=5)
        self.assertEqual(frame.asset.get_attr(sorrow_attr), 1)
        self.assertEqual(frame.asset.get_attr(surprise_attr), 1)
        self.assertAlmostEqual(frame.asset.get_attr(face_attr),
                               -5.038763046264648, places=5)
        self.assertEqual(frame.asset.get_attr(ex_attr), 1)

    @patch.object(ZmlpClient, 'upload_file')
    @patch(patch_path, side_effect=MockImageAnnotatorClient)
    def test_detect_image_text(self, mock_image_annotator, upload_patch):
        upload_patch.return_value = PROXY_FILE
        # initialize the asset and processor
        asset = TestAsset(STREETSIGN)
        frame = Frame(asset)
        store_asset_proxy(asset, STREETSIGN, (200, 200))
        processor = self.init_processor(CloudVisionProcessor(), {
            "detect_image_text": True,
            "detect_document_text": False,
            "detect_landmarks": False,
            "detect_explicit": False,
            "detect_faces": False,
            "detect_labels": False,
            "detect_logos": False,
            "detect_objects": False,
            "detect_web_entities": False
        })

        # run processor with declared frame and assert asset attributes
        processor.process(frame)
        sign_content = u'PASEO TAMAYO\nNIRVANA 6400 N\nNO OUTLET\u2192\nSTOP\n'
        asset_attr = "analysis.google.imageTextDetection.content"
        self.assertEqual(frame.asset.get_attr(asset_attr), sign_content)

    @patch.object(ZmlpClient, 'upload_file')
    @patch(patch_path, side_effect=MockImageAnnotatorClient)
    def test_detect_document_text(self, mock_image_annotator, upload_patch):
        self.maxDiff = None
        upload_patch.return_value = PROXY_FILE
        # initialize the asset and processor
        asset = TestAsset(MANUAL)
        frame = Frame(asset)
        store_asset_proxy(asset, MANUAL, (200, 200))
        processor = self.init_processor(CloudVisionProcessor(), {
            "detect_image_text": False,
            "detect_document_text": True,
            "detect_landmarks": False,
            "detect_explicit": False,
            "detect_faces": False,
            "detect_labels": False,
            "detect_logos": False,
            "detect_objects": False,
            "detect_web_entities": False
        })
        manual_text = u'Notice\nThe information provided in this ' \
                      u'specification ' \
                      u'is believed to be accurate and ' \
                      u'reliable as of the date provided. ' \
                      u'However, NVIDIA\nCorporation ("NVIDIA") ' \
                      u'does not give ' \
                      u'any representations or warranties, ' \
                      u'expressed or implied, as to the ' \
                      u'accuracy or completeness ' \
                      u'of\nsuch information. NVIDIA shall ' \
                      u'have no liability for the consequences ' \
                      u'or use of such ' \
                      u'information or for any infringement ' \
                      u'of patents\nor other rights of third ' \
                      u'parties that may' \
                      u' result from its use. This publication ' \
                      u'supersedes and replaces all other ' \
                      u'specifications for ' \
                      u'the\nproduct that may have been ' \
                      u'previously supplied.\nNVIDIA reserves ' \
                      u'the right to make ' \
                      u'corrections, modifications, ' \
                      u'enhancements, improvements, and ' \
                      u'other changes to this ' \
                      u'specification,\nat any time and/or ' \
                      u'to discontinue any product or ' \
                      u'service without notice. ' \
                      u'Customer should obtain the latest ' \
                      u'relevant specification\nbefore placing ' \
                      u'orders and should ' \
                      u'verify that such information is current ' \
                      u'and complete.\nNVIDIA products are sold ' \
                      u'subject to ' \
                      u'the NVIDIA standard terms and conditions ' \
                      u'of sale supplied at the time of ' \
                      u'order\nacknowledgement, unless otherwise ' \
                      u'agreed in an individual sales agreement ' \
                      u'signed by ' \
                      u'authorized representatives of NVIDIA ' \
                      u'and\ncustomer. NVIDIA hereby expressly ' \
                      u'objects to ' \
                      u'applying any customer general terms ' \
                      u'and conditions with regard to the ' \
                      u'purchase ' \
                      u'of\nthe NVIDIA ' \
                      u'product referenced in this ' \
                      u'specification.\nNVIDIA products ' \
                      u'are not designed, authorized or ' \
                      u'warranted to be suitable for use ' \
                      u'in medical, military, aircraft, ' \
                      u'space or life ' \
                      u'support\nequipment, nor in ' \
                      u'applications where failure or ' \
                      u'malfunction of the NVIDIA ' \
                      u'product can reasonably be expected ' \
                      u'to result in personal\ninjury, ' \
                      u'death or property or ' \
                      u'environmental damage. NVIDIA accepts ' \
                      u'no liability for inclusion ' \
                      u'and/or use of NVIDIA products ' \
                      u'in such\nequipment or applications ' \
                      u'and therefore such inclusion ' \
                      u'and/or use is at customer\'s ' \
                      u'own risk.\nNVIDIA makes no ' \
                      u'representation or warranty that ' \
                      u'products based on ' \
                      u'these specifications will be ' \
                      u'suitable for any specified use ' \
                      u'without\nfurther ' \
                      u'testing or modification. Testing ' \
                      u'of all parameters of each product ' \
                      u'is not necessarily ' \
                      u'performed by NVIDIA. It is ' \
                      u'customer\'s\nsole responsibility ' \
                      u'to ensure the product is' \
                      u' suitable and fit for the ' \
                      u'application planned by customer ' \
                      u'and to do the necessary ' \
                      u'testing\nfor the application ' \
                      u'in order to avoid a default ' \
                      u'of the application or the product. ' \
                      u'Weaknesses in customer\'s ' \
                      u'product designs may ' \
                      u'affect\nthe quality and ' \
                      u'reliability of the ' \
                      u'NVIDIA product and may result in ' \
                      u'additional or different ' \
                      u'conditions and/or requirements ' \
                      u'beyond\nthose contained ' \
                      u'in this specification. ' \
                      u'NVIDIA does not accept ' \
                      u'any liability related ' \
                      u'to any default, damage, ' \
                      u'costs or problem ' \
                      u'which\nmay be based ' \
                      u'on or attributable ' \
                      u'to: ' \
                      u'(i) the use of ' \
                      u'the NVIDIA product ' \
                      u'in any manner that ' \
                      u'is contrary to ' \
                      u'this specification, or ' \
                      u'(ii)\ncustomer product ' \
                      u'designs.\nNo license, ' \
                      u'either expressed or ' \
                      u'implied, is granted under ' \
                      u'any NVIDIA patent ' \
                      u'right, copyright, or other NVIDIA ' \
                      u'intellectual property\nright under this ' \
                      u'specification. ' \
                      u'Information published ' \
                      u'by NVIDIA regarding ' \
                      u'third-party products ' \
                      u'or services' \
                      u' does not constitute ' \
                      u'a\nlicense from NVIDIA ' \
                      u'to use such ' \
                      u'products or services ' \
                      u'or a warranty or' \
                      u' endorsement thereof. ' \
                      u'Use of such information ' \
                      u'may require a\nlicense ' \
                      u'from a third party ' \
                      u'under the patents or ' \
                      u'other intellectual property rights of the ' \
                      u'third party, or a ' \
                      u'license from ' \
                      u'NVIDIA under ' \
                      u'the\npatents or ' \
                      u'other intellectual ' \
                      u'property rights of ' \
                      u'NVIDIA. Reproduction of ' \
                      u'information in this ' \
                      u'specification is ' \
                      u' only if\nreproduction ' \
                      u'is approved by NVIDIA' \
                      u' in writing, is reproduced ' \
                      u'without alteration, ' \
                      u'and is accompanied by all ' \
                      u'associated ' \
                      u'conditions,\nlimitations, ' \
                      u'and notices.\nALL ' \
                      u'NVIDIA DESIGN SPECIFICATIONS, ' \
                      u'REFERENCE BOARDS, FILES, ' \
                      u'DRAWINGS, DIAGNOSTICS, ' \
                      u'LISTS, AND OTHER\nDOCUMENTS ' \
                      u'(TOGETHER AND SEPARATELY, ' \
                      u'"MATERIALS") ARE BEING ' \
                      u'PROVIDED "AS IS." NVIDIA ' \
                      u'MAKES ' \
                      u'NO WARRANTIES,\nEXPRESSED, ' \
                      u'IMPLIED, STATUTORY, OR ' \
                      u'OTHERWISE WITH RESPECT ' \
                      u'TO THE MATERIALS, ' \
                      u'AND EXPRESSLY DISCLAIMS ' \
                      u'ALL\nIMPpLIED WARRANTIES ' \
                      u'OF NONINFRINGEMENT, MERCHANTABILITY, AND ' \
                      u'FITNESS FOR A PARTICULAR ' \
                      u'PURPOSE.\nNotwithstanding ' \
                      u'any damages that customer ' \
                      u'might incur ' \
                      u'for any reason whatsoever, ' \
                      u'NVIDIA\'s aggregate and ' \
                      u'cumulative liability\ntowards ' \
                      u'customer for ' \
                      u'the products described herein ' \
                      u'shall be limited in accordance ' \
                      u'with the NVIDIA terms and ' \
                      u'conditions of sale for\nthe ' \
                      u'product.\nVESA ' \
                      u'DisplayPort\nDisplayPort ' \
                      u'and DisplayPort' \
                      u' Compliance Logo, DisplayPort ' \
                      u'Compliance Logo for ' \
                      u'Dual-mode Sources, and DisplayPort ' \
                      u'Compliance\nLogo for ' \
                      u'Active Cables are ' \
                      u'trademarks owned ' \
                      u'by the Video Electronics Standards' \
                      u' Association in the United States and ' \
                      u'other\ncountries.\nHDMI\nHDMI, the HDMI logo, and' \
                      u' High-Definition ' \
                      u'Multimedia Interface ' \
                      u'are trademarks or ' \
                      u'registered ' \
                      u'trademarks of HDMI ' \
                      u'Licensing LLC.\nOpenCL\nOpenCL ' \
                      u'is a trademark of Apple Inc. ' \
                      u'used under license to the' \
                      u' Khronos Group ' \
                      u'Inc.\nTrademarks\nNVIDIA, ' \
                      u'the NVIDIA logo, CUDA, ' \
                      u'Game Works, GameStream, ' \
                      u'GeForce, GeForce GTX, GeForce ' \
                      u'Experience, Giga Thread, ' \
                      u'GPU-\nBOOST, G-Sync, Lumenex, ' \
                      u'PureVideo, PhysX, ShadowPlay, ' \
                      u'and TXAA are trademarks or ' \
                      u'registered trademarks ' \
                      u'of NVIDIA\nCorporation. ' \
                      u'Other company product names ' \
                      u'may be trademarks of the ' \
                      u'respective' \
                      u' companies with which they ' \
                      u'are associated.\nCopyright\n\xa9 ' \
                      u'2016 NVIDIA Corporation. ' \
                      u'All rights reserved.\nNVIDIA\n'

        # run the cloud vision processor and assert asset attributes
        processor.process(frame)
        asset_attr = "analysis.google.documentTextDetection.content"
        self.assertEqual(frame.asset.get_attr(asset_attr), manual_text)
