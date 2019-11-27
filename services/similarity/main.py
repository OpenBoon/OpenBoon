from flask import Flask, request, Response
import jsonpickle
import numpy as np
import cv2
import mxnet
from collections import namedtuple

app = Flask(__name__)
sym, arg_params, aux_params = mxnet.model.load_checkpoint("resnet-152", 0)
mod = mxnet.mod.Module(symbol=sym, context=mxnet.cpu(), label_names=[])
mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
mod.set_params(arg_params, aux_params, allow_missing=True)


@app.route('/')
def test():
    return('Hello World')


@app.route('/api/hash', methods=['POST'])
def hash():
    r = request
    print('###########')
    print(r.data)
    print('###########')

    # convert string of image data to uint8
    nparr = np.fromstring(r.data, np.uint8)

    print(nparr)
    # decode image
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    img = cv2.resize(img, (224, 224))
    img = np.swapaxes(img, 0, 2)
    img = np.swapaxes(img, 1, 2)
    img = img[np.newaxis, :]

    Batch = namedtuple('Batch', ['data'])

    all_layers = sym.get_internals()
    fe_sym = all_layers['flatten0_output']
    fe_mod = mxnet.mod.Module(symbol=fe_sym, context=mxnet.cpu(), label_names=[])
    fe_mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
    fe_mod.set_params(arg_params, aux_params)

    fe_mod.forward(Batch([mxnet.nd.array(img)]))
    features = fe_mod.get_outputs()[0].asnumpy()
    features = np.squeeze(features)

    mxh = np.clip((features * 16).astype(int), 0, 15) + 65

    mxhash = "".join([chr(item) for item in mxh])
    mxhash = '0050F' + mxhash
    # build a response dict to send back to client
    response = {'shash': mxhash}

    # encode response using jsonpickle
    response_pickled = jsonpickle.encode(response)

    return Response(response=response_pickled, status=200, mimetype="application/json")


if __name__ == "__main__":
    app.run()
