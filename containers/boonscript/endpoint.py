import flask

from utils import asset_from_request


def setup(app):

    @app.route('/', methods=['POST'])
    def endpoint():
        asset = asset_from_request()

        # do some stuff

        # Make a response
        rsp = {
            'analysis': [
                {
                    "type": "labels",
                    "predictions": [
                        {
                            "label": "cat",
                            "score": 0.99
                        }
                    ]
                }
            ],
            'custom': {
                'some_field1': "some_value",
                'some_field2': "some_value",
            }
        }

        return flask.jsonify(rsp)
