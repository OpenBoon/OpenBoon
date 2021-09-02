import re
import logging

import langcodes

logger = logging.getLogger(__name__)


class LanguageCodes:

    iso_6391_to_bcp47 = {
        'af': 'af-ZA',
        'ar': 'ar-SA',
        'zh': 'zh-CN',
        'da': 'da-DK',
        'nl': 'nl-NL',
        'en': 'en-US',
        'fr': 'fr-FR',
        'de': 'de-DE',
        'he': 'he-IL',
        'hi': 'hi-IN',
        'id': 'id-ID',
        'it': 'it-IT',
        'jp': 'ja-JP',
        'ko': 'ko-KR',
        'ms': 'ms-MY',
        'pt': 'pt-BR',
        'ru': 'ru-RU',
        'es': 'es-ES',
        'ta': 'ta-IN',
        'th': 'th-TH',
        'tr': 'th-TR'
    }

    lang_regex = re.compile(r"lang[-/]([a-z]{2}[\-_][a-z]{2})", re.IGNORECASE)
    """
    A regex used to match a lang code in file name or path.
    """

    lang_defaults = ['en-US', 'fr-FR', 'es-US', 'pt-BR']
    """The default lang list"""

    @classmethod
    def to_bcp47(cls, lang):
        """
        Tries to take something like 'en' or 'eng' and convert it to a
        preferred BCP 47 lang tag.  We have a limited lookup map based on
        what is supported by AWS.

        Args:
            lang (str): A ISO 639-2 lang code.

        Returns:
            list: The BCP 47 lang code or none.
        """
        if '-' in lang:
            return [lang]
        try:
            small_code = langcodes.get(lang).language
            bcpcode = LanguageCodes.iso_6391_to_bcp47.get(small_code)
            if bcpcode:
                return [bcpcode]
        except langcodes.tag_parser.LanguageTagError as e:
            logger.warning(f'Unable to convert lang {lang} to a BCP47 code', e)

        return None

    @classmethod
    def detect_in_string(cls, data):
        """
        Detect a lang code in string. Generally this is in a file path like:

           * /foo/bar/lang/en-US/file.mp4
           * /foo/bar/lang-en-US/file.mp4
           * /foo/bar/lang-en_us/file.mp4
           * /foo/bar/lang-en-US-file.mp4

        Args:
            data (str): The string to detect.

        Returns:
            list: The detected lang code or none.

        """
        match = cls.lang_regex.search(data)
        if match:
            lang = match.group(1)
            try:
                tag = langcodes.standardize_tag(lang)
                return [tag]
            except langcodes.tag_parser.LanguageTagError as e:
                logger.warning(f'Unable to convert lang {lang} to a BCP47 code', e)
        return None
