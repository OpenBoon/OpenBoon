"""Low-level archivist client support functions and objects."""

import atexit
import datetime
import json
import logging
import os
import random
import sys
import time
import traceback
import decimal
import uritools

from hashlib import md5

import jwt
import requests

from . import exception

logger = logging.getLogger(__name__)

if sys.platform == "darwin":
    from requests.packages.urllib3.exceptions import InsecureRequestWarning

    requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
else:
    import urllib3

    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


class ZorroaJsonEncoder(json.JSONEncoder):
    """
    JSON encoder for with Zorroa specific serialization defaults.
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


class KeyManager(object):
    """
    The KeyManager class handles the storage and selection of API keys. This
    entails handling any user-specified server/username pairs, authentication
    environment.
    """

    def __init__(self, server, user, keydir=None):
        """
        Create a new KeyManager.

        Order of operations for the keydir property:
            - Use the keydir argument if given
            - Use the value at the ZORROA_KEY_DIR environment variable, if it exists
            - Use $HOME/.zorroa

        Args:
            server (str): The server to connect to. Pass None to connect to default server.
            user (str): The user to connect as. Pass None to utilize the default user.
            keydir (str): The directory where API keys are stored.  Defaults to ~/.zorroa
        """
        # These values are used to lookup an API key, if any.
        self.arg_server = self._normalize_url(server)
        self.arg_user = user
        self.server = None
        self.user = None

        # A cached auth-token
        self.auth_token = None

        # Set to true if the token was obtained via username and password.
        self.session_token = False

        default_key_dir = os.path.join(os.environ.get("HOME"), ".zorroa")
        self.keydir = keydir or os.getenv("ZORROA_KEY_DIR", default_key_dir)

        # These values are not used to lookup the API key, they are simply
        # used to make the connection.
        self._set_connection_defaults()

        if logger.getEffectiveLevel() == logging.DEBUG:
            logger.debug("KeyManager args: arg_server=%s arg_user=%s" %
                         (self.arg_server, self.arg_user))
            logger.debug("KeyManager defaults: server=%s user=%s" %
                         (self.server, self.user))

    def _set_connection_defaults(self):
        """
        Set the Archivist connection defaults.

        Order of operations for server URL:
            - Utilize the arg_server property if set.
            - Utilize the ZORROA_ARCHIVIST_URL environment variable.
            - Fallback to "https://localhost/"

        Order of operations for the user:
            - Utilize the arg_user property if set.
            - Utilize the ZORROA_USER environment variable.
            - Fallback to "admin"

        If a keyfile is detected it is loaded here.

        """
        # Grab the defaults from the command line or env.
        server = self.arg_server or os.environ.get("ZORROA_ARCHIVIST_URL", "https://localhost/")
        user = self.arg_user or os.environ.get("ZORROA_USER", "admin")
        self.server = self._normalize_url(server)
        self.user = user

        # Call get auth-token to override server/user using default key.
        self.get_auth_token()

    def get_auth_token(self):
        """
        Obtain a JWT token for communication with the Archivist.

        Order of operations:
            - Check the ZORROA_AUTH_TOKEN env var for a bearer token.
            - Utilize server+user arguments to select the ApiKey.
            - Utilize the default ApiKey if configured.

        Returns:
            str: A JWT bearer token.
        """
        # Utilize a hard coded auth-token.
        if self.auth_token:
            return self.auth_token
        # If there is an auth-token already set, we don't need to do anything.
        elif os.environ.get('ZORROA_AUTH_TOKEN'):
            return "Bearer {}".format(os.environ.get('ZORROA_AUTH_TOKEN'))
        else:

            # key_data is the HMAC key we need to calculate our signing request.
            # If we don't find a key using a variety of fallback method then
            # the client is likely to fail unless manually authenticated.
            key_data = None
            try:
                # Get a key file based on args or the marked default
                key_file_path = self.auto_detect_key_file_path()
                if key_file_path:
                    with open(key_file_path, "r") as fp:
                        key_data = json.load(fp)

                if key_data:
                    claims = {'aud': key_data['server'],
                              'exp': datetime.datetime.utcnow() + datetime.timedelta(seconds=60),
                              'userId': key_data['userId']}
                    token = jwt.encode(claims, key_data['key'], algorithm='HS256')

                    self.server = key_data["server"]
                    self.user = key_data["user"]

                    if logger.getEffectiveLevel() == logging.DEBUG:
                        logger.debug("Detected API key for server=%s user=%s" %
                                     (self.server, self.user))

                    return "Bearer {}".format(token)

            except Exception as e:
                if logger.getEffectiveLevel() == logging.DEBUG:
                    logger.debug("Failed to load any form of API key, unexpected %s" % e)
                    # We'll need this for quick troubleshooting in case anything goes wrong.
                    traceback.print_exc()

            if logger.getEffectiveLevel() == logging.DEBUG:
                logger.debug("Did not load API key, no API keys configured or match args.")

            return None

    def auto_detect_key_file_path(self):
        """
        Return the path to the detected key file.  You must either specify connection args
        which override the key detection, or the default key will be selected.  If
        the connection arg key does not exist, the default WILL NOT be selected automatically.
        This way there is no question what server you are connecting to.

        Returns:
            str: The path to the detected key file.

        """
        # If args are set, then the default keyfile cannot be used.
        # It has to be selected via the args.
        if self.arg_user and self.arg_server:
            return self.get_arg_key_file_path()
        # Otherwise choose the default key if no args are set.
        else:
            return self.get_default_key_file_path()

    def get_default_key_file_path(self):
        """
        Return the path to the API key file marked as the default.

        Returns:
            str: The path to the API key file.

        """
        path = os.path.join(self.keydir, "default_key")
        if os.path.exists(path):
            return os.path.realpath(os.path.join(self.keydir, "default_key"))
        return ""

    def get_arg_key_file_path(self):
        """
        Return the path to the API key file using the user/server arguments passed
        by the user.

        Returns:
            str: The path to the API key file.

        """
        if not self.arg_server or not self.arg_user:
            return ""
        kid = self.get_key_signature(self.arg_server, self.arg_user)
        if kid:
            path = os.path.join(self.keydir, kid + ".key")
            if os.path.exists(path):
                return path
        return ""

    def get_key_file_path(self, kid):
        """
        Return the full path to the given API key Id.

        Args:
            kid (str): The API key unique ID.

        Returns:
            str: The path to the API key file.

        """
        return os.path.join(self.keydir, kid + ".key")

    def create_key_file(self, key_data):
        """
        Create a API key file for the given url and user with the base key-data.

        Args:
            key_data (dict): base key data returned from the Archivist

        Returns:
            str: The path to the API key file.

        """
        try:
            os.makedirs(self.keydir, 0o700)
        except OSError:
            pass

        kid = self.get_key_signature(key_data["server"], key_data["user"])
        key_file_path = os.path.join(self.keydir, kid + ".key")
        if os.path.exists(key_file_path):
            os.unlink(key_file_path)
        with os.fdopen(os.open(key_file_path, os.O_WRONLY | os.O_CREAT, 0o600),
                       'w') as handle:
            handle.write(json.dumps(key_data))
        return key_file_path

    def get_key_signature(self, server, user):
        """
        Generate a key signature from the given server and user values.

        Args:
            server (str): The URL to the server.
            user (str): The username.

        Returns:
            str: a key file signature string.

        """
        if not server:
            raise ValueError("Cannot generate a key signature without a server URL.")
        if not user:
            raise ValueError("Cannot generate a key signature without a username.")
        server = self._normalize_url(server)
        if not server.startswith("http") and server.endswith("/"):
            raise ValueError("Invalid server URL: %s" % server)
        return md5(("%s::%s" % (user, server)).encode('utf-8')).hexdigest()

    def get_all_keys(self):
        """
        Return a list of all key files.

        Returns:
            list[str]: A list of API key file paths.

        """
        return [key for key in os.listdir(self.keydir) if key.endswith(".key")]

    def select_default_key(self, kid):
        """Auto-select a specific key.

        Args:
            kid(str): The key ID

        """
        kid = kid.replace(".key", "")
        key_file = self.get_key_file_path(kid)
        if os.path.exists(key_file):
            active_link = os.path.join(self.keydir, "default_key")
            if os.path.exists(active_link):
                os.unlink(active_link)
            os.symlink(os.path.basename(key_file), active_link)

    def _normalize_url(self, url):
        """
        Ensures that any given URL ends with a /.  This is necessary for unique
        API key file generation.

        Args:
            url (str): A URL

        Returns:
            str: a modified url

        """
        if url and url[-1] != "/":
            url = url + "/"
        return url


class ZHttpClient(object):
    """
    Client is responsible for making REST calls to the Archivist and
    interpreting the result.

    Args:
        server (str): The url of the server to contact.
        user (str): The username to use.

    Attributes:
        default_max_retries (int): The default number of maximum retries
            the client will make after a connection error.  Defaults to 3.
            Changing this will change the default for all new Client instances.
    """

    default_max_retries = 3

    def __init__(self, server=None, user=None, max_retries=None):
        self.__key_manager = KeyManager(server, user)
        self.__max_retries = max_retries
        self.__organization = os.environ.get("ZORROA_OVERRIDE_ORGANIATION_ID")
        atexit.register(self.logout)

    @property
    def user(self):
        """
        The user this client is configured with.

        Returns:
             str: The user

        """
        return self.__key_manager.user

    @property
    def server(self):
        """
        The server this client is configured with

        Returns:
             str: The server URL

        """
        return self.__key_manager.server

    @property
    def key_manager(self):
        """
        Return the current key manager instance.

        Returns:
             KeyManager: The current :class:`KeyManager`

        """
        return self.__key_manager

    @property
    def max_retries(self):
        """
        Return the number of retries the Client will make upon a connection
        error.

        If no max_retries property was supplied with the constructor,
        checks the
        ZORROA_ARCHIVIST_MAX_RETRIES env var.  If the env var is not set, the
        client falls back on the default_max_retries class variable.

        A zero or non positive value will yield infinite retries.

        Returns
            int: Number of retries
        """
        return self.__max_retries or int(
            os.environ.get("ZORROA_ARCHIVIST_MAX_RETRIES",
                           self.default_max_retries))

    @property
    def organization(self):
        """
        The organization ID the client is operating on. The value is None
        if the default Organization for the user is being used.

        Returns:
            str: The organization ID.

        """
        return self.__organization

    def set_organization(self, org_id):
        """
        Set the client to operate on the given organization ID.  Set it back to None to operate
        on the users default organization.

        Args:
            org_id (str): An organization Id.

        """
        self.__organization = org_id

    def setup_key_authentication(self, server, user):
        """
        This method allows you to set the Set HMAC key authentication values
        after the class has been constructed. A new KeyManager will
        be created.

        Args:
            server (str): The url of the server to contact.  Can be None.
            user (str): The username to use. Can be None.

        """
        self.__key_manager = KeyManager(server, user)

    def authenticate(self, user, password, server=None):
        """
        Authenticate with Archivist using a username and password. Optionally,
        provide a new server URL as well.

        Args:
            user (str): The username to authenticate with.
            password (str): The password to authenticate with.
            server (str): An optional server to connect with. Leaving null
                assumes an existing server.
        """
        self.logout()

        # If the auth argument is set, we go through usernane.password
        # authentication.
        # Otherwise just regular key authentication.
        self.__key_manager = KeyManager(server or self.server, user)

        arg_map = {"auth": (user, password)}
        try:
            arg_map["headers"] = self.headers(with_token=False)
            arg_map["verify"] = False
            rsp = requests.post(self.get_url("api/v1/login"), **arg_map)
            if "X-Zorroa-Credential" in rsp.headers:
                self.__key_manager.auth_token = rsp.headers["X-Zorroa-Credential"]
                self.__key_manager.session_token = True
            self.__handle_rsp(rsp, True)
        except Exception as e:
            raise exception.ArchivistConnectionException(e)

    def logout(self):
        """
        Attempt to log out and invalidate the HTTP session.
        """
        # Only logout if the token was obtained by providing a username and password
        if self.__key_manager:
            if self.__key_manager.session_token:
                try:
                    self.post("api/v1/logout")
                except Exception as e:
                    logger.debug("Failed to logout: %s", e)

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
                    raise exception.ArchivistRequestException(
                        "Failed to stream asset: %s" % response)

                for block in response.iter_content(1024):
                    handle.write(block)
            return dst
        except requests.exceptions.ConnectionError as e:
            raise exception.ArchivistConnectionException(e)

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
                raise exception.ArchivistRequestException(
                    "Failed to stream asset: %s" % response)

            for line in response.iter_lines():
                if line:
                    yield (line)

        except requests.exceptions.ConnectionError as e:
            raise exception.ArchivistConnectionException(e)

    def get(self, path, body=None, is_json=True):
        """
        Performs a get request using the authenticated archivist session.
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
        Performs a post request using the authenticated archivist session.
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
        Performs a put request using the authenticated archivist session.
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
         Performs a delete request using the authenticated archivist session.
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
            data = encode(body)
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
                msg = "Communicating to Archivist (%s) timed out %d times, " \
                      "waiting ... %d seconds, error=%s\n"
                sys.stderr.write(msg % (url, tries, wait, e))
                time.sleep(wait)

        return self.__handle_rsp(rsp, is_json)

    def upload(self, path, files, body={}, json_rsp=True, field="files"):
        """
        Upload the given list of file paths to the Archivist.

        Args:
            path (str): The URL to upload to
            files (list): The file paths to upload
            body (dict): A request body
            json_rsp (bool): Set to true if the result returned is JSON
            field (string): The multi-part form field to use for the files.
        """
        try:
            post_files = []
            if not isinstance(files, (list, tuple, set)):
                files = [files]
            for f in files:
                post_files.append(
                    (field, (os.path.basename(f), open(f, 'rb'))))

            if body is not None:
                post_files.append(
                    ["body", (None, json.dumps(body), 'application/json')])

            return self.__handle_rsp(requests.post(
                self.get_url(path), headers=self.headers(content_type=""),
                files=post_files), json_rsp)

        except requests.exceptions.ConnectionError as e:
            raise exception.ArchivistConnectionException(e)

    def register_key(self, replace=False):
        """
        Register or generate new HMAC key for the current user and server.

        Args:
            replace (bool): regenerate and replace the user's existing server
                side key.

        Returns:
             tuple: A tuple of the path to the, and the key structure
                itself.

        """
        api_key = self.post("api/v1/users/api-key",
                            {"replace": replace, "server": self.server},
                            is_json=True)
        key_path = self.__key_manager.create_key_file(api_key)
        return key_path, json.load(open(key_path))

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

        # If the status code can't be found, then ArchivistRequestException is returned.
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
        if with_token:
            token = self.__key_manager.get_auth_token()
            if token:
                header['Authorization'] = token

        if self.__organization:
            header["X-Zorroa-Organization"] = self.__organization

        if content_type:
            header['Content-Type'] = content_type

        index_route = os.environ.get("ZORROA_INDEX_ROUTE_ID")
        if index_route:
            header["X-Zorroa-Index-Route"] = index_route

        if logger.getEffectiveLevel() == logging.DEBUG:
            logger.debug("headers: %s" % header)

        return header


def connect(username, password, server=None):
    """
    Connect to given Archivist URL with the specified username and password.

    Args:
        username (str): username to connect with.
        password (str): optional password to connect with.  If a password is
            set, JWT auth is not used.
        server (str): An optional url for the server. If not server is not
            set, the default URL is used.
    """
    _client.authenticate(username, password, server)


def who():
    """
    Return information for current user.
    """
    return _client.get("/api/v1/who")


def encode(obj):
    return json.dumps(obj, cls=ZorroaJsonEncoder)


def get_client():
    """
    Return the global auto-configured client instance.
    """
    return _client


# A global client instance.
_client = ZHttpClient()
