import os
import zipfile

from zmlp_train.utils import models

cur_dir = os.path.abspath(os.path.dirname(__file__))


def test_zip_directory():
    output_zip = "/tmp/foo.zip"
    models.zip_directory(cur_dir + "/../../tf2/tests", output_zip)

    zip = zipfile.ZipFile(output_zip)
    for name in zip.namelist():
        assert name.startswith("tests")
    roses = [n for n in zip.namelist() if 'roses' in n]
    assert 6 == len(roses)
    daisy = [n for n in zip.namelist() if 'daisy' in n]
    assert 6 == len(daisy)
