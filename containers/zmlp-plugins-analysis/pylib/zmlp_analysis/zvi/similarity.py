import os
from collections import namedtuple

import cv2
import mxnet
import numpy as np

from zmlpsdk import AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path

package_directory = os.path.dirname(os.path.abspath(__file__))

Batch = namedtuple('Batch', ['data'])


class ZviSimilarityProcessor(AssetProcessor):
    """
    make a hash with ResNet
    """

    namespace = "zvi-image-similarity"

    # MXNet is not thread safe.
    use_threads = False

    model_path = "/models/resnet-152"

    def __init__(self):
        super(ZviSimilarityProcessor, self).__init__()
        self.mod = None
        self.sym = None
        self.arg_params = None
        self.aux_params = None

    def init(self):
        self.sym, self.arg_params, self.aux_params = mxnet.model.load_checkpoint(self.model_path +
                                                                                 "/resnet-152", 0)
        self.mod = self.load_model()

    def load_image(self, asset):
        p_path = get_proxy_level_path(asset, 0)
        if not p_path:
            self.logger.warning("No proxy available for ZmlpSimilarityResNet152")
            return
        img = cv2.imread(p_path)
        img = cv2.resize(img, (224, 224))
        if img.shape == (224, 224):
            img = cv2.cvtColor(img, cv2.CV_GRAY2RGB)
        img = np.swapaxes(img, 0, 2)
        img = np.swapaxes(img, 1, 2)
        img = img[np.newaxis, :]
        return mxnet.nd.array(img)

    def load_model(self):
        all_layers = self.sym.get_internals()
        fe_sym = all_layers['flatten0_output']
        fe_mod = mxnet.mod.Module(symbol=fe_sym, context=mxnet.cpu(), label_names=None)
        fe_mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
        fe_mod.set_params(self.arg_params, self.aux_params)
        return fe_mod

    def process(self, frame):
        asset = frame.asset

        self.mod.forward(Batch([self.load_image(asset)]))
        features = self.mod.get_outputs()[0].asnumpy()
        features = np.squeeze(features)

        mxh = np.clip((features*16).astype(int), 0, 15) + 65
        mxhash = "".join([chr(item) for item in mxh])
        mxhash = mxhash
        struct = {
            'type': 'similarity',
            'simhash': mxhash
        }

        self.add_analysis(asset, self.namespace, struct)
