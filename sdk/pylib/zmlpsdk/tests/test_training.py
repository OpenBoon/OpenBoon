import os.path as path

from zmlpsdk.training import (
    get_labels,
    load_image,
    extract_model,
    id_generator,
)

cur_dir = path.abspath(path.dirname(__file__))
test_dir = path.abspath(cur_dir)


def test_get_labels():
    labels = get_labels(test_dir, "ClassifierTest_labels.txt")
    assert len(labels) == 6


def test_extract_model():
    model_zip = path.join(test_dir, "model.zip")
    loc = extract_model(model_zip)

    assert path.exists(loc)
    assert path.exists(path.join(loc, "fake_model.dat"))
    assert path.exists(path.join(loc, "mode-version.txt"))


def test_load_image():
    img_path = path.join(test_dir, "test_dsy.jpg")
    img = load_image(img_path)

    assert img.shape == (1, 224, 224, 3)


def test_id_generator():
    simhash = id_generator()

    assert len(simhash) == 6  # default length
    assert simhash.isalpha() and simhash.isupper()  # default alpha uppercase
