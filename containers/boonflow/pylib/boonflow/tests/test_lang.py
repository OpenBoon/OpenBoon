from boonflow.lang import LanguageCodes


def test_detect_in_string():
    assert LanguageCodes.detect_in_string('/foo/bar/lang/en-US/foo.mp4') == ['en-US']
    assert LanguageCodes.detect_in_string('/foo/bar/lang/en_us/foo.mp4') == ['en-US']
    assert LanguageCodes.detect_in_string('/foo/bar/lang-en-US/foo.mp4') == ['en-US']
    assert LanguageCodes.detect_in_string('/foo/bar/lang-en-us-foo.mp4') == ['en-US']
    assert LanguageCodes.detect_in_string('/foo/bar/no/lang.mp4') is None


def test_to_bcp47():
    assert LanguageCodes.to_bcp47("en") == ['en-US']
    assert LanguageCodes.to_bcp47("eng") == ['en-US']
    assert LanguageCodes.to_bcp47("fr") == ['fr-FR']
    assert LanguageCodes.to_bcp47("deu") == ['de-DE']
