import base64
import binascii
import datetime
import decimal
import json
import logging
import os
import random
import sys
import time

import jwt
import requests
import uritools
import urllib3

from . import exception

logger = logging.getLogger(__name__)

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


class ZmlpClient(object):
    """
    ZmlpClient is used to communicate to a ZMLP API server.
    """

    def __init__(self, apikey, server='https://api.zmlp.zorroa.com', **kwargs):
        """
        Create a new ZmlpClient instance.

        Args:
            apikey: An API key in any supported form. (dict, base64 string, or open file handle)
            server: The url of the server to connect to. Defaults to https://api.zmlp.zorroa.com
            project_id: An optional project UUID for API keys with access to multiple projects.
            max_retries: Maximum number of retries to make if the API
                server is down, 0 for unlimited.
        """
        self.apikey = self.__load_apikey(apikey)
        self.server = server
        self.project_id = kwargs.get('project_id')
        self.max_retries = kwargs.get('max_retries', 3)

    def stream(self, url, dst):
        """
        Stream the given URL path to local dst file path.

        Args:
            url (str): The URL to stream
            dst (str): The destination file path
        """
        try:
            with open(dst, 'wb') as handle:
                response = requests.get(self.get_url(url), verify=False,
                                        headers=self.headers(), stream=True)

                if not response.ok:
                    raise exception.ZmlpInvalidRequestException(
                        "Failed to stream asset: %s" % response)

                for block in response.iter_content(1024):
                    handle.write(block)
            return dst
        except requests.exceptions.ConnectionError as e:
            raise exception.ZmlpConnectionException(e)

    def stream_text(self, url):
        """
        Stream the given URL.

        Args:
            url (str): The URL to stream

        Yields:
            generator (str): A generator of the lines making up the textual
                URL.
        """
        try:
            response = requests.get(self.get_url(url), verify=False,
                                    headers=self.headers(), stream=True)
            if not response.ok:
                raise exception.ZmlpRequestException(
                    "Failed to stream asset: %s" % response)

            for line in response.iter_lines():
                if line:
                    yield (line)

        except requests.exceptions.ConnectionError as e:
            raise exception.ZmlpConnectionException(e)

    def get(self, path, body=None, is_json=True):
        """
        Performs a get request.
        Args:
            path (str): An archivist URI path.
            body (object): The request body which will be serialized to json.
            is_json (bool): Set to true to specify a JSON return value

        Returns:
            object: The http response object or an object deserialized from the
                response json if the ``json`` argument is true.

        Raises:
            Exception: An error occurred making the request or parsing the
                JSON response
        """
        return self._make_request('get', path, body, is_json)

    def post(self, path, body=None, is_json=True):
        """
        Performs a post request.
        Args:
            path (str): An archivist URI path.
            body (object): The request body which will be serialized to json.
            is_json (bool): Set to true to specify a JSON return value

        Returns:
            object: The http response object or an object deserialized from the
                response json if the ``json`` argument is true.

        Raises:
            Exception: An error occurred making the request or parsing the
                JSON response
        """
        return self._make_request('post', path, body, is_json)

    def put(self, path, body=None, is_json=True):
        """
        Performs a put request.
        Args:
            path (str): An archivist URI path.
            body (object): The request body which will be serialized to json.
            is_json (bool): Set to true to specify a JSON return value

        Returns:
            object: The http response object or an object deserialized from the
                response json if the ``json`` argument is true.

        Raises:
            Exception: An error occurred making the request or parsing the
                JSON response
        """
        return self._make_request('put', path, body, is_json)

    def delete(self, path, body=None, is_json=True):
        """
         Performs a delete request.
         Args:
             path (str): An archivist URI path.
             body (object): The request body which will be serialized to json.
             is_json (bool): Set to true to specify a JSON return value

         Returns:
             object: The http response object or an object deserialized from
             the response json if the ``json`` argument is true.

         Raises:
             Exception: An error occurred making the request or parsing the
                JSON response
         """
        return self._make_request('delete', path, body, is_json)

    def iter_paged_results(self, url, req, maxitems, cls):
        """
        Handles paging through the results of the standard _search
        endpoints on the backend.

        Args:
            url (str): the URL to POST a search to
            req (object): the search request body
            maxitems (int): the maximum items to return
            cls (type): the class to wrap each result in

        Yields:
            Generator

        """
        left_to_return = maxitems or sys.maxsize
        page = 0
        req["page"] = {}
        while True:
            if left_to_return < 1:
                break
            page += 1
            req["page"]["size"] = min(50, left_to_return)
            req["page"]["from"] = (page - 1) * req["page"]["size"]
            rsp = self.post(url, req)
            if not rsp.get("list"):
                break
            for f in rsp["list"]:
                yield (cls(f))
                left_to_return -= 1
            # Used to break before pulling new batch
            if rsp.get("break"):
                break

    def _make_request(self, method, path, body=None, is_json=True):
        request_function = getattr(requests, method)
        if body is not None:
            data = json.dumps(body, cls=ZorroaJsonEncoder)
        else:
            data = body

        # Making the request is wrapped in its own try/catch so it's easier
        # to catch any and all socket and http exceptions that can possibly be
        # thrown.  Once hat happens, handle_rsp is called which may throw
        # application level exceptions.
        rsp = None
        tries = 0
        url = self.get_url(path, body)
        while True:
            try:
                rsp = request_function(url, data=data, headers=self.headers(),
                                       verify=False)
                break
            except Exception as e:
                # Some form of connection error, wait until archivist comes
                # back.
                tries += 1
                if 0 < self.max_retries <= tries:
                    raise e
                wait = random.randint(1, random.randint(1, 60))
                # Switched to stderr in case no logger is setup, still want
                # to see messages.
                msg = "Communicating to ZMLP (%s) timed out %d times, " \
                      "waiting ... %d seconds, error=%s\n"
                sys.stderr.write(msg % (url, tries, wait, e))
                time.sleep(wait)

        return self.__handle_rsp(rsp, is_json)

    def __handle_rsp(self, rsp, is_json):
        if rsp.status_code != 200:
            self.__raise_exception(rsp)
        if is_json and len(rsp.content):
            rsp_val = rsp.json()
            if logger.getEffectiveLevel() == logging.DEBUG:
                logger.debug(
                    "rsp: status: %d  body: '%s'" % (rsp.status_code, rsp_val))
            return rsp_val
        return rsp

    def __raise_exception(self, rsp):
        data = {}
        try:
            data.update(rsp.json())
        except Exception as e:
            # The result is not json.
            data["message"] = "Your HTTP request was invalid '%s', response not " \
                              "JSON formatted. %s" % (rsp.status_code, e)
            data["status"] = rsp.status_code

        # If the status code can't be found, then ZmlpRequestException is returned.
        ex_class = exception.translate(rsp.status_code)
        raise ex_class(data)

    def get_url(self, path, body=None):
        """
        Returns the full URL including the configured server part.
        """
        url = uritools.urijoin(self.server, path)
        if logger.getEffectiveLevel() == logging.DEBUG:
            logger.debug("url: '%s' path: '%s' body: '%s'" % (url, path, body))
        return url

    def headers(self, with_token=True, content_type="application/json"):
        """
        Generate the return some request headers.

        Args:
            with_token (bool): Set to true if JWT token should be included in
                the header.  Defaults to true.
            content_type(str):  The content-type for the request. Defaults to
                'application/json'

        Returns:
            dict: An http header struct.

        """
        header = {}
        header['Authorization'] = "Bearer {}".format(self.__sign_request())

        if content_type:
            header['Content-Type'] = content_type

        if logger.getEffectiveLevel() == logging.DEBUG:
            logger.debug("headers: %s" % header)

        return header

    def __load_apikey(self, apikey):
        key_data = None
        if hasattr(apikey, 'read'):
            key_data = json.load(apikey)
        elif isinstance(apikey, dict):
            key_data = apikey
        elif isinstance(apikey, str):
            try:
                key_data = json.loads(base64.b64decode(apikey))
            except binascii.Error:
                raise ValueError("Invalid base64 encoded API key.")

        return key_data

    def __sign_request(self):
        if not self.apikey:
            raise exception.ZmlpInvalidRequestException("Unable to make requeest,"
                                                        "No ApiKey has been specified.")
        claims = {
            'aud': self.server,
            'exp': datetime.datetime.utcnow() + datetime.timedelta(seconds=60),
            'keyId': self.apikey["keyId"]
        }
        if self.project_id:
            claims["projectId"] = self.project_id
        return jwt.encode(claims, self.apikey['sharedKey'], algorithm='HS512').decode("utf-8")


def from_env():
    """
    Create a ZmlpClient configured via environment variables. This method
    will not throw if the environment is configured improperly, however
    attempting the use the ZmlpClient instance to make a request
    will fail.

    - ZMLP_APIKEY : A base64 encoded API key.
    - ZMLP_APIKEY_FILE : A path to a JSON formatted API key.
    - ZMLP_SERVER : The URL to the ZMLP API server.

    Returns:
        ZmlpClient : A configured ZmlpClient

    """
    apikey = None
    if 'ZMLP_APIKEY' in os.environ:
        bytes = base64.b64decode(os.environ['ZMLP_APIKEY'])
        apikey = json.loads(bytes.decode())
    elif 'ZMLP_APIKEY_FILE' in os.environ:
        with open(os.environ['ZMLP_APIKEY_FILE'], 'r') as fp:
            apikey = json.load(fp)

    server = os.environ.get('ZMLP_SERVER', 'https://api.zmlp.zorroa.com')
    return ZmlpClient(apikey, server)


class ZorroaJsonEncoder(json.JSONEncoder):
    """
    JSON encoder for with ZMLP specific serialization defaults.
    """

    def default(self, obj):
        if hasattr(obj, 'for_json'):
            return obj.for_json()
        elif isinstance(obj, (set, frozenset)):
            return list(obj)
        elif isinstance(obj, datetime.datetime):
            return obj.strftime("%Y-%m-%d %H:%M:%S %z").strip()
        elif isinstance(obj, datetime.date):
            return obj.strftime("%Y-%m-%d")
        elif isinstance(obj, datetime.time):
            return obj.strftime("%H:%M:%S %z").strip()
        elif isinstance(obj, decimal.Decimal):
            return float(obj)

        # Let the base class default method raise the TypeError
        return json.JSONEncoder.default(self, obj)
