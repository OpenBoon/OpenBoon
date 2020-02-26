import os
from collections import namedtuple

import cv2
import mxnet
import numpy as np
from pathlib2 import Path

from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.proxy import get_proxy_level_path

package_directory = os.path.dirname(os.path.abspath(__file__))


class ZviLabelDetectionResNet152(AssetProcessor):
    """
    Classify with ResNet
    """
    toolTips = {
        'debug': 'Run in debug mode. This creates a few extra fields, including confidence values.'
    }

    namespace = 'zvi.label-detection'

    def __init__(self):
        super(ZviLabelDetectionResNet152, self).__init__()
        self.add_arg(Argument("debug", "boolean", default=False, toolTip=self.toolTips['debug']))
        self.labels = []
        self.mod = None

    def init(self):
        self.model_path = str(Path(__file__).parent.joinpath('models/resnet-152'))
        sym, self.arg_params, self.aux_params = mxnet.model.load_checkpoint(self.model_path +
                                                                            "/resnet-152", 0)
        self.mod = mxnet.mod.Module(symbol=sym, context=mxnet.cpu(), label_names=None)
        self.mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
        self.mod.set_params(self.arg_params, self.aux_params, allow_missing=True)
        with open(self.model_path + '/synset.txt', 'r') as f:
            self.labels = [l.rstrip() for l in f]

    def process(self, frame):
        asset = frame.asset

        p_path = get_proxy_level_path(asset, 0)
        if not p_path:
            self.logger.warning("No proxy available for ZmlpLabelDetectionResNet152")
        img = cv2.imread(p_path)
        img = cv2.resize(img, (224, 224))
        img = np.swapaxes(img, 0, 2)
        img = np.swapaxes(img, 1, 2)
        img = img[np.newaxis, :]

        Batch = namedtuple('Batch', ['data'])

        self.mod.forward(Batch([mxnet.nd.array(img)]))
        prob = self.mod.get_outputs()[0].asnumpy()
        prob = np.squeeze(prob)

        # psort is a sorting of prob. We need to keep prob in order to assign
        # the floating point probabilities attrs
        psort = np.argsort(prob)[::-1]
        kw = []
        for j, i in enumerate(psort[0:5]):
            kw.extend([k.strip() for k in self.labels[i].split(',') if k])

        struct = {
            'labels': list(set(kw)),
            'score': float(prob[psort[0]])
        }

        # Debug info, if enabled.
        if self.arg_value('debug'):
            struct['debug'] = {
                'type': 'mxnet',
                'model': os.path.basename(self.model_path),
            }
            for j, i in enumerate(psort[0:5]):
                struct['debug']['pred' + str(j)] = ','.join(
                    self.labels[i].replace(',', '').split(' ')[1:])
                struct['debug']['prob' + str(j)] = prob[i]

        asset.add_analysis(self.namespace, struct)


class ZviSimilarityProcessor(AssetProcessor):
    """
    make a hash with ResNet
    """

    namespace = "zvi.similarity"

    def __init__(self):
        super(ZviSimilarityProcessor, self).__init__()
        self.labels = []
        self.mod = None
        self.sym = None
        self.arg_params = None
        self.aux_params = None

    def init(self):
        self.model_path = str(Path(__file__).parent.joinpath('models/resnet-152'))
        # self.model_path = self.context.shared_data.model_path + '/mxnet/resnet-152'
        self.sym, self.arg_params, self.aux_params = mxnet.model.load_checkpoint(self.model_path +
                                                                                 "/resnet-152", 0)
        self.mod = mxnet.mod.Module(symbol=self.sym, context=mxnet.cpu(), label_names=None)
        self.mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
        self.mod.set_params(self.arg_params, self.aux_params, allow_missing=True)
        with open(self.model_path + '/synset.txt', 'r') as f:
            self.labels = [l.rstrip() for l in f]

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
            'simhash': mxhash
        }

        asset.add_analysis(self.namespace, struct)
