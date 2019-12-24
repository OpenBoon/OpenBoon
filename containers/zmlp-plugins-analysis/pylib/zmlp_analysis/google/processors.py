import io
import os
import re
import backoff
import tempfile
import subprocess

from pathlib2 import Path

from google.cloud import vision
from google.cloud.vision import types
from google.cloud import language
from google.cloud import videointelligence_v1p2beta1 as videointelligence
from google.cloud import speech_v1p1beta1 as speech
from google.api_core.exceptions import ResourceExhausted

from .base import GoogleApiDocumentProcessor
from .base import AutoMLModelProcessor

from zmlp.analysis import Argument, ZmlpFatalProcessorException
from zmlp.analysis.storage import get_proxy_level


class CloudVisionProcessor(GoogleApiDocumentProcessor):
    """Use Google Cloud Vision API to analyze images."""

    tool_tips = {
        'detect_image_text': 'If True, Text Detection performs Optical Character Recognition.'
                             'It detects and extracts text within an image with support for a'
                             'broad range of languages. It also features automatic language'
                             'identification.',
        'detect_document_text': 'If True, Document Text Detection performs Optical Character'
                                'Recognition. This feature detects dense document text -'
                                'including handwriting - in an image.',
        'detect_landmarks': 'If True, Landmark Detection detects popular natural and man-made'
                            'structures within an image.',
        'detect_explicit': 'If True, Safe Search Detection detects explicit content such as'
                           'adult content or violent content within an image. This feature'
                           'uses five categories ("adult", "spoof", "medical", "violence",'
                           'and "racy") and returns the likelihood that each is present in'
                           'a given image',
        'detect_faces': 'If True Face Detection detects multiple faces within an image along'
                        'with the associated key facial attributes such as emotional state'
                        'or wearing headwear.',
        'detect_labels': 'If True Label Detection detects broad sets of categories within an'
                         'image, which range from modes of transportation to animals.',
        'detect_web_entities': 'If True, Web Detection detects Web references to an image.',
        'detect_logos': 'If True, Logo Detection detects popular product logos within an image.',
        'detect_objects': 'If True, Object Localization can detect and extract multiple objects'
                          'in an image.',
    }

    def __init__(self):
        super(CloudVisionProcessor, self).__init__()
        self.add_arg(Argument('detect_image_text', 'bool', default=True,
                              toolTip=self.tool_tips['detect_image_text']))
        self.add_arg(Argument('detect_document_text', 'bool', default=True,
                              toolTip=self.tool_tips['detect_document_text']))
        self.add_arg(Argument('detect_landmarks', 'bool', default=True,
                              toolTip=self.tool_tips['detect_landmarks']))
        self.add_arg(Argument('detect_explicit', 'bool', default=True,
                              toolTip=self.tool_tips['detect_explicit']))
        self.add_arg(Argument('detect_faces', 'bool', default=True,
                              toolTip=self.tool_tips['detect_faces']))
        self.add_arg(Argument('detect_labels', 'bool', default=True,
                              toolTip=self.tool_tips['detect_labels']))
        self.add_arg(Argument('detect_web_entities', 'bool', default=True,
                              toolTip=self.tool_tips['detect_web_entities']))
        self.add_arg(Argument('detect_logos', 'bool', default=True,
                              toolTip=self.tool_tips['detect_logos']))
        self.add_arg(Argument('detect_objects', 'bool', default=True,
                              toolTip=self.tool_tips['detect_objects']))
        self.add_arg(Argument('debug', 'bool', default=False))
        self.image_annotator = None

    def init(self):
        super(CloudVisionProcessor, self).init()
        self.image_annotator = self.initialize_gcp_client(vision.ImageAnnotatorClient)

    def process(self, frame):
        asset = frame.asset
        path = get_proxy_level(asset, 1)
        if not path:
            return
        if Path(path).stat().st_size > 10485760:
            raise ZmlpFatalProcessorException(
                'The image is too large to submit to Google ML. Image size must '
                'be < 10485760 bytes')
        with io.open(path, 'rb') as image_file:
            content = image_file.read()
        image = types.Image(content=content)
        if self.arg_value('detect_image_text'):
            self._detect_image_text(asset, image)
        if self.arg_value('detect_document_text'):
            self._detect_document_text(asset, image)
        if self.arg_value('detect_landmarks'):
            self._detect_landmarks(asset, image)
        if self.arg_value('detect_explicit'):
            self._detect_explicit(asset, image)
        if self.arg_value('detect_faces'):
            self._detect_faces(asset, image)
        if self.arg_value('detect_labels'):
            self._detect_labels(asset, image)
        if self.arg_value('detect_web_entities'):
            self._detect_web_entities(asset, image)
        if self.arg_value('detect_logos'):
            self._detect_logos(asset, image)
        if self.arg_value('detect_objects'):
            self._detect_objects(asset, image)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_image_text(self, asset, image):
        """Executes text detection on images using the Cloud Vision API."""
        response = self.image_annotator.text_detection(image=image)
        text = response.full_text_annotation.text
        if text:
            if len(text) > 32766:
                text = text[:32765]
            asset.add_analysis('google.imageTextDetection', {'content': text})

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_document_text(self, asset, image):
        """Executes text detection using the Cloud Vision API."""
        response = self.image_annotator.document_text_detection(image=image)
        text = response.full_text_annotation.text
        if text:
            if len(text) > 32766:
                text = text[:32765]
            asset.add_analysis('google.documentTextDetection', {'content': text})

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_landmarks(self, asset, image):
        """Executes landmark detection using the Cloud Vision API."""
        response = self.image_annotator.landmark_detection(image=image)
        landmarks = response.landmark_annotations
        if landmarks:
            # We'll only add the first landmark found for now.
            struct = {}
            landmark = landmarks[0]
            struct['keywords'] = landmark.description
            struct['point'] = (landmark.locations[0].lat_lng.longitude,
                               landmark.locations[0].lat_lng.latitude)
            struct['score'] = float(landmark.score)
            self.logger.info('Storing: {}'.format(struct))
            asset.add_analysis("google.landmarkDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_explicit(self, asset, image):
        """Executes safe-search detection using the Cloud Vision API."""
        response = self.image_annotator.safe_search_detection(image=image)
        safe = response.safe_search_annotation
        struct = {}
        for category in ['adult', 'spoof', 'medical', 'violence', 'racy']:
            rating = getattr(safe, category)
            if rating < 1 or rating > 5:
                continue
            score = (float(rating) - 1.0) * 0.25
            struct[category] = score
        if struct:
            asset.add_analysis("google.explicit", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_faces(self, asset, image):
        """Executes face detection using the Cloud Vision API."""
        response = self.image_annotator.face_detection(image=image)
        faces = response.face_annotations
        if faces:

            # We'll only add the first face found for now. TODO: deal with multiple faces
            face = faces[0]
            struct = {}
            keywords = []
            struct['roll_angle'] = face.roll_angle
            struct['pan_angle'] = face.pan_angle
            struct['tilt_angle'] = face.tilt_angle
            struct['detection_confidence'] = face.detection_confidence
            struct['joy_likelihood'] = face.joy_likelihood
            struct['sorrow_likelihood'] = face.sorrow_likelihood
            struct['anger_likelihood'] = face.anger_likelihood
            struct['surprise_likelihood'] = face.surprise_likelihood
            struct['under_exposed_likelihood'] = face.under_exposed_likelihood
            struct['blurred_likelihood'] = face.blurred_likelihood
            struct['headwear_likelihood'] = face.headwear_likelihood

            # For entries that are likely or very likely, add the attribute name as a keyword.
            for key, value in struct.items():
                if 'likelihood' in key and value >= 4:
                    keywords.append(key.replace('_likelihood', ''))

            # This deduplication is unnecessary now but will be necessary when multiple
            # faces are accounted for
            struct["keywords"] = list(set(keywords))

            asset.add_analysis("google.faceDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_labels(self, asset, image):
        """Executes label detection using the Cloud Vision API."""
        response = self.image_annotator.label_detection(image=image)
        labels = response.label_annotations
        struct = {}
        keywords = []
        for i, label in enumerate(labels):
            keywords.append(label.description)
            if self.arg_value("debug"):
                struct["pred" + str(i)] = label.description
                struct["prob" + str(i)] = labels[i].score
        struct["keywords"] = list(set(keywords))
        if self.arg_value("debug"):
            struct["type"] = "GCLabelDetection"
            struct["scores"] = labels[0].score
        asset.add_analysis("google.visionLabelDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_web_entities(self, asset, image):
        """Executes web entity detection using the Cloud Vision API.
            Image size limit of 10mb
            Args:
                asset(Asset): Frame asset
                image(Image): Image content
        """
        response = self.image_annotator.web_detection(image=image)
        annotations = response.web_detection
        struct = {}
        keywords = []
        for i, entity in enumerate(annotations.web_entities):
            # skipping null values prevent images from being omitted from zvi
            if entity.description:
                keywords.append(entity.description)
                if self.arg_value("debug"):
                    struct["pred" + str(i)] = entity.description
                    struct["prob" + str(i)] = entity.score
        struct["keywords"] = list(set(keywords))
        asset.add_analysis("google.webEntityDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_logos(self, asset, image):
        """Executes logo detection using the Cloud Vision API.
            Image size limit of 10mb
            Args:
                asset(Asset): Frame asset
                image(Image): Image content
        """
        response = self.image_annotator.logo_detection(image=image)
        logos = response.logo_annotations
        struct = {}
        keywords = []
        for i, logo in enumerate(logos):
            # skipping null values prevent images from being omitted from zvi
            if logo.description:
                keywords.append(logo.description)
                if self.arg_value("debug"):
                    struct["pred" + str(i)] = logo.description
                    struct["prob" + str(i)] = logo.score
        struct["keywords"] = list(set(keywords))
        asset.add_analysis("google.logoDetection", struct)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _detect_objects(self, asset, image):
        """Executes logo detection using the Cloud Vision API.
            Image size limit of 10mb
            Args:
                asset(Asset): Frame asset
                image(Image): Image content
        """
        response = self.image_annotator.object_localization(image=image)
        objects = response.localized_object_annotations
        struct = {}
        keywords = []
        for i, object in enumerate(objects):
            # skipping null values prevent images from being omitted from zvi
            if object.name:
                keywords.append(object.name)
                if self.arg_value("debug"):
                    struct["pred" + str(i)] = object.name
                    struct["prob" + str(i)] = object.score
        struct["keywords"] = list(set(keywords))
        asset.add_analysis("google.objectDetection", struct)


class CloudVideoIntelligenceProcessor(GoogleApiDocumentProcessor):
    """Use Google Cloud Video Intelligence API to label videos."""

    file_types = ['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg']

    tool_tips = {
        'detect_labels': 'If True then label detection will be run.',
        'detect_text': 'If True OCR will be run.'
    }

    def __init__(self):
        super(CloudVideoIntelligenceProcessor, self).__init__()
        self.add_arg(Argument('detect_labels', 'bool', default=True,
                              toolTip=self.tool_tips['detect_labels']))
        self.add_arg(Argument('detect_text', 'bool', default=True,
                              toolTip=self.tool_tips['detect_text']))
        self.video_intel_client = None

    def init(self):
        super(CloudVideoIntelligenceProcessor, self).init()
        self.video_intel_client = self.initialize_gcp_client(
            videointelligence.VideoIntelligenceServiceClient)

    def process(self, frame):
        asset = frame.asset
        if not asset.attr_exists("clip"):
            self.logger.info('Skipping this frame, it is not a video clip.')
            return
        clip_contents = self._get_clip_bytes(asset)
        annotation_result = self._get_video_annotations(clip_contents)
        if self.arg_value('detect_labels'):
            self._add_video_intel_labels(asset, 'google.videoLabel.segment.keywords',
                                         annotation_result.segment_label_annotations)
            self._add_video_intel_labels(asset, 'google.videoLabel.shot.keywords',
                                         annotation_result.shot_label_annotations)
            self._add_video_intel_labels(asset, 'google.videoLabel.frame.keywords',
                                         annotation_result.frame_label_annotations)
        if self.arg_value('detect_text'):
            text = ' '.join(t.text for t in annotation_result.text_annotations)
            if text:
                asset.add_analysis('google.videoText.content', text)

    def _get_clip_bytes(self, asset):
        """Gets the in/out points for the clip specified in the metadata and transcodes
        that section into a new video. The bytes of that new video file are returned.

        Args:
            asset (Asset): Asset that has clip metadata.

        Returns:
            str: Byte contents of the video clip that was created.

        """
        clip_start = float(asset.get_attr('media.clip.start'))
        clip_length = float(asset.get_attr('media.clip.length'))
        video_length = asset.get_attr('media.duration')
        seek = max(clip_start - 0.25, 0)
        duration = min(clip_length + 0.5, video_length)
        clip_path = Path(tempfile.mkdtemp(),
                         next(tempfile._get_candidate_names()) + '.mp4')

        # Construct ffmpeg command line
        command = ['ffmpeg',
                   '-i', str(asset.get_local_source_path()),
                   '-ss', str(seek),
                   '-t', str(duration),
                   '-s', '512x288',
                   str(clip_path)]
        self.logger.info('Executing: %s' % command)
        subprocess.check_call(command)

        # Read the extracted video file
        with clip_path.open('rb') as _file:
            return _file.read()

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=5 * 60 * 60)
    def _get_video_annotations(self, video_contents):
        """Uses the Google Video Intelligence API to get video annotations.

        Args:
            video_contents (str): Contents of a video file to send to the Google API.

        Returns:
            VideoAnnotationResults: Results from Google API.

        """
        features = []
        if self.arg_value('detect_labels'):
            features.append(videointelligence.enums.Feature.LABEL_DETECTION)
        if self.arg_value('detect_text'):
            features.append(videointelligence.enums.Feature.TEXT_DETECTION)
        operation = self.video_intel_client.annotate_video(input_content=video_contents,
                                                           features=features,)
        return operation.result(timeout=500).annotation_results[0]

    def _add_video_intel_labels(self, asset, analysis_field, annotations):
        """Extracts labels from a Google VideoAnnotationResults object and adds them to
        the metadata of an Asset.

        Args:
            asset (Asset): Asset to add metadata to.
            analysis_field (str): Metadata field in the "analysis" namespace to add
             values to.
            annotations (VideoAnnotationResults): Results from a call to Cloud Video
             Intelligence API to get labels from.

        """
        keywords = []
        for annotation in annotations:
            keywords.append(annotation.entity.description)
            for category_entity in annotation.category_entities:
                keywords.append(category_entity.description)
        if keywords:
            asset.add_analysis(analysis_field, keywords)


class CloudSpeechToTextProcessor(GoogleApiDocumentProcessor):
    file_types = ['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg', 'aac', 'mp3',
                  'flac', 'wav']

    tool_tips = {'overwrite_existing': 'If the metadata this processor creates is already set on an'
                                       'asset, processing of this asset is skipped. (Default: '
                                       'False)',
                 'primary_language': 'A ISO 639-1 standard language code indicating the primary '
                                     'language to be expected in the given assets. (Default: '
                                     '"en-US")',
                 'alternate_languages': 'Up to 10 ISO 639-1 language codes indicating potential'
                                        'secondary languages found in the assets being processed.'}

    def __init__(self):
        super(CloudSpeechToTextProcessor, self).__init__()
        self.add_arg(Argument('overwrite_existing', 'bool', default=False,
                              toolTip=self.tool_tips['overwrite_existing']))
        self.add_arg(Argument('primary_language', 'string', default='en-US',
                              toolTip=self.tool_tips['primary_language']))
        self.add_arg(Argument('alternate_languages', 'list', default=[],
                              toolTip=self.tool_tips['alternate_languages']))
        self.speech_client = None
        self.audio_channels = 2
        self.audio_sample_rate = 44100

    def init(self):
        super(CloudSpeechToTextProcessor, self).init()
        self.speech_client = self.initialize_gcp_client(speech.SpeechClient)

    def process(self, frame):
        asset = frame.asset
        analysis_field = 'google.speechRecognition'
        if not asset.attr_exists("clip"):
            self.logger.warning('Skipping, this asset is not a clip.')
            return
        if not self.arg_value('overwrite_existing') and asset.get_attr('analysis.%s' %
                                                                       analysis_field):
            self.logger.warning('Skipping, this asset has already been processed.')
        audio = speech.types.RecognitionAudio(content=self._get_audio_clip_content(asset))
        attributes = self._recognize_speech(audio)
        # if no speech was recognized, attributes == None
        if attributes:
            asset.add_analysis(analysis_field, attributes)
        else:
            self.logger.info('Asset contains no discernible speech.')
            asset.add_analysis(analysis_field, None)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _recognize_speech(self, audio):
        config = speech.types.RecognitionConfig(
            encoding=speech.enums.RecognitionConfig.AudioEncoding.FLAC,
            audio_channel_count=self.audio_channels,
            sample_rate_hertz=self.audio_sample_rate,
            language_code=self.arg_value('primary_language'),
            alternative_language_codes=self.arg_value('alternate_languages'),
            max_alternatives=10)
        response = self.speech_client.recognize(config=config, audio=audio)
        confidence = 0
        content = ''
        language = ''
        pieces = 0
        for r in response.results:
            language = r.language_code
            best_alt_confidence = 0
            best_alt_transcript = ''
            for alt in r.alternatives:
                if alt.confidence > best_alt_confidence:
                    best_alt_confidence = alt.confidence
                    best_alt_transcript = alt.transcript
            confidence += best_alt_confidence
            content += ' ' + best_alt_transcript
            pieces += 1
        if pieces == 0:
            return None
        confidence /= pieces
        return {'language': language, 'confidence': confidence, 'content': content}

    def _get_audio_clip_content(self, asset):
        clip_start = asset.get_attr('media.clip.start')
        clip_length = asset.get_attr('media.clip.length')
        video_length = asset.get_attr('media.duration')
        seek = max(clip_start - 0.25, 0)
        duration = min(clip_length + 0.5, video_length)
        self.logger.info('Original time in & duration: {}, {}'.format(clip_start, clip_length))
        self.logger.info('Expanded time in & duration: {}, {}'.format(seek, duration))
        audio_fname = os.path.join(tempfile.gettempdir(),
                                   next(tempfile._get_candidate_names())) + ".flac"

        # Construct ffmpeg command line
        cmd_line = ['ffmpeg',
                    '-i', asset.get_local_source_path(),
                    '-vn',
                    '-acodec', 'flac',
                    '-ar', str(self.audio_sample_rate),
                    '-ac', str(self.audio_channels),
                    '-ss', str(seek),
                    '-t', str(duration),
                    audio_fname]

        self.logger.info('Executing %s' % cmd_line)
        subprocess.check_call(cmd_line)
        with io.open(audio_fname, 'rb') as audio_file:
            return audio_file.read()


class CloudNaturalLanguageProcessor(GoogleApiDocumentProcessor):
    """Use Google Cloud Natural Language API to analyze a text field in the metadata."""

    tool_tips = {'field': 'Metadata field of the asset to submit to the Cloud Natural Language '
                          'API.'}

    def __init__(self):
        super(CloudNaturalLanguageProcessor, self).__init__()
        self.add_arg(Argument('field', 'str', default='media.dialog',
                              toolTip=self.tool_tips['field']))
        self.client = None

    def init(self):
        super(CloudNaturalLanguageProcessor, self).init()
        self.client = self.initialize_gcp_client(language.LanguageServiceClient)

    def flatten_content(self, content):
        """Recursively flattens list(s) of strings into a single space-delimited string.

        Args:
            content (list or str): List of strings to flatten.

        Returns:
            str: Flattened string of all content.

        """
        flat = ''
        if type(content) == list:
            for elem in content:
                flat += ' ' + self.flatten_content(elem)
            return flat
        if type(content) == str or type(content) == str:
            return content

    def process(self, frame):
        asset = frame.asset
        content = asset.get_attr(self.arg_value('field'))
        self.logger.info('Content: {}'.format(content))
        if content is None:
            self.logger.info('Bailing, no content found')
            return
        content = self.flatten_content(content)

        # Get rid of CC parentheticals
        content = re.sub(r'\[.*?\]', '', content)

        self.logger.info('Content: {}'.format(content))
        document = language.types.Document(content=content,
                                           type=language.enums.Document.Type.PLAIN_TEXT)
        result = self._analyze_entities(document)
        entities = []
        for entity in result.entities:
            entities.append(entity.name)
        if not entities:
            self.logger.info('Bailing, no entities found')
            return
        self.logger.info('Entities: {}'.format(entities))
        asset.add_analysis('google.languageEntities.keywords', entities)

    @backoff.on_exception(backoff.expo, ResourceExhausted, max_time=10 * 60)
    def _analyze_entities(self, document):
        """Wraps call to Cloud Natural Language API in exponential backoff decorator."""
        return self.client.analyze_entities(document)


class AutoMLVisionModelProcessor(AutoMLModelProcessor):
    def _announce(self, asset):
        self.logger.info("AutoMLVisionModelProcessor for asset {}".format(asset.id))

    def _proc_fieldname(self):
        return 'automl_vision'

    def _create_payload(self, asset):
        file_path = get_proxy_level(asset, 1)
        with open(file_path, 'rb') as fh:
            content = fh.read()

        self.logger.info("\tRead {} bytes from {}".format(len(content), file_path))
        # self.logger.debug("\tContent is: {}".format(str(content)))
        return {
            "image": {
                "image_bytes": content
            }
        }


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
        # TODO: a pixml search instead.
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
