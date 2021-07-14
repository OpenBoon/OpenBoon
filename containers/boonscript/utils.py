import json

import flask

from boonsdk import Asset


def asset_from_request():
    return Asset(json.loads(flask.request.data))
