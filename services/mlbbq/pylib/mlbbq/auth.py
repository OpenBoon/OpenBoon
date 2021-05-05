import os

import flask
import requests

auth_url = os.environ.get("BOONAI_SECURITY_AUTHSERVER_URL", "http://auth-server:9090")
auth_endpoint = "{}/auth/v1/auth-token".format(auth_url)


def authenticate():
    """
    Authenticate the given token.  Abort with a 403 if authentication fails.
    """
    token = flask.request.headers.get("Authorization")
    rsp = requests.post(auth_endpoint, headers={"Authorization": token})
    if rsp.status_code != 200:
        flask.abort(403, description="Access is denied")
    return rsp.json()


def check_write_access():
    """
    Checks to see if the current auth has the AssetsImport permission and
    raises a 403 if it does not.
    """
    actor = authenticate()
    if 'AssetsImport' not in actor['permissions']:
        flask.abort(403, description="Access is denied")
    return actor


def check_read_access():
    """
    Checks to see if the current auth has the AssetsRead permission and
    raises a 403 if it does not.
    """
    actor = authenticate()
    if 'AssetsRead' not in actor['permissions']:
        flask.abort(403, description="Access is denied")
    return actor
