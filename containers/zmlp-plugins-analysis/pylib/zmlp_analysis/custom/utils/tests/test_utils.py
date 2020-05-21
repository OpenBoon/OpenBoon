import os.path as path

from zmlp_analysis.custom.utils.utils import (
    get_labels,
    load_image,
    extract_model,
)

cur_dir = path.abspath(path.dirname(__file__))
test_dir = path.abspath(path.join(cur_dir, "../../", "tests/"))


def test_get_labels():
    labels = get_labels(test_dir, "ClassifierTest_labels.txt")
    assert len(labels) == 6


def test_extract_model():
    model_zip = path.join(test_dir, "model.zip")
    loc = extract_model(model_zip)

    assert path.exists(loc)
    assert path.exists(path.join(loc, "model/"))


def test_load_image():
    img_path = path.join(test_dir, "test_dsy.jpg")
    img = load_image(img_path)

    assert img.shape == (1, 224, 224, 3)
