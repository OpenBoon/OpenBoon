import os
from collections import namedtuple

import cv2
import mxnet
import numpy as np

from zmlpsdk import AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path

package_directory = os.path.dirname(os.path.abspath(__file__))


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
        self.labels = []
        self.mod = None
        self.sym = None
        self.arg_params = None
        self.aux_params = None

    def init(self):
        self.sym, self.arg_params, self.aux_params = mxnet.model.load_checkpoint(self.model_path +
                                                                                 "/resnet-152", 0)
        self.mod = mxnet.mod.Module(symbol=self.sym, context=mxnet.cpu(), label_names=None)
        self.mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
        self.mod.set_params(self.arg_params, self.aux_params, allow_missing=True)
        with open(self.model_path + '/synset.txt', 'r') as f:
            self.labels = [l1.rstrip() for l1 in f]

    def process(self, frame):
        asset = frame.asset
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

        Batch = namedtuple('Batch', ['data'])

        all_layers = self.sym.get_internals()
        fe_sym = all_layers['flatten0_output']
        fe_mod = mxnet.mod.Module(symbol=fe_sym, context=mxnet.cpu(), label_names=None)
        fe_mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
        fe_mod.set_params(self.arg_params, self.aux_params)

        fe_mod.forward(Batch([mxnet.nd.array(img)]))
        features = fe_mod.get_outputs()[0].asnumpy()
        features = np.squeeze(features)

        mxh = np.clip((features*16).astype(int), 0, 15) + 65
        mxhash = "".join([chr(item) for item in mxh])

        mxhash = mxhash
        struct = {
            'type': 'similarity',
            'simhash': mxhash
        }

        asset.add_analysis(self.namespace, struct)
