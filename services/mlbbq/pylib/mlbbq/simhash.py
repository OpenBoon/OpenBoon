import os
import threading
from collections import namedtuple

import cv2
import mxnet
import numpy
import numpy as np


class SimilarityModel:
    model = os.path.dirname(__file__) + "/resnet-152"
    sym, arg_params, aux_params = mxnet.model.load_checkpoint(model, 0)
    mod = mxnet.mod.Module(symbol=sym, context=mxnet.cpu(), label_names=None)
    mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
    mod.set_params(arg_params, aux_params, allow_missing=True)
    lock = threading.Lock()


def get_similarity_hash(data):
    """

    Args:
        data:

    Returns:

    """
    img = cv2.imdecode(numpy.fromstring(data.read(), numpy.uint8), cv2.IMREAD_UNCHANGED)
    img = cv2.resize(img, (224, 224))
    if img.shape == (224, 224):
        img = cv2.cvtColor(img, cv2.CV_GRAY2RGB)
    img = np.swapaxes(img, 0, 2)
    img = np.swapaxes(img, 1, 2)
    img = img[np.newaxis, :]

    # Mxnet is not thread safe.
    with SimilarityModel.lock:
        Batch = namedtuple('Batch', ['data'])

        all_layers = SimilarityModel.sym.get_internals()
        fe_sym = all_layers['flatten0_output']
        fe_mod = mxnet.mod.Module(symbol=fe_sym, context=mxnet.cpu(), label_names=None)
        fe_mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
        fe_mod.set_params(SimilarityModel.arg_params, SimilarityModel.aux_params)

        fe_mod.forward(Batch([mxnet.nd.array(img)]))
        features = fe_mod.get_outputs()[0].asnumpy()
        features = np.squeeze(features)

        mxh = np.clip((features * 16).astype(int), 0, 15) + 65
        mxhash = "".join([chr(item) for item in mxh])

        return mxhash
