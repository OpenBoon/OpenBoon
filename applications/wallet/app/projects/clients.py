import base64
import json
from urllib.parse import urljoin

import jwt
import requests
from requests import Response


class ZviClient(object):
    def __init__(self, apikey, server):
        key_info = json.loads(base64.b64decode(apikey))
        self.user_id = key_info['userId']
        self.root_url = server
        self.key = key_info['key']

    def _get_token(self):
        claims = {'aud': self.root_url, 'userId': self.user_id}
        return jwt.encode(claims, self.key, algorithm='HS256')

    def _get_headers(self) -> dict:
        """Returns request headers needed to make requests to the REST API."""
        return {'Authorization': f'Bearer {self._get_token()}',
                'Content-Type': 'application/json',
                'companyId': '0'}

    def get(self, path: str) -> Response:
        """Makes a GET request to a REST API.

            Args:
                path: Endpoint to make the POST request to.

            Returns: Response from the request.

        """
        path = urljoin(self.root_url, path)
        response = requests.get(path, headers=self._get_headers())
        response.raise_for_status()
        return response

    def post(self, path: str, data: dict, extra_headers: dict = None) -> Response:
        """Makes a POST request to a REST API.

            Args:
                path: Endpoint to make the POST request to.
                data: Data to send in the body as json.
                extra_headers: Any additional headers that should be added to the request.

            Returns: Response from the request.

        """
        url = urljoin(self.root_url, path)
        headers = self._get_headers()
        if extra_headers:
            headers.update(extra_headers)
        response = requests.post(url, json=data, headers=headers)
        response.raise_for_status()
        return response

    def put(self, path: str, data: dict, extra_headers: dict = None) -> Response:
        """Makes a PUT request to a REST API.

            Args:
                path: Endpoint to make the POST request to.
                data: Data to send in the body as json.
                extra_headers: Any additional headers that should be added to the request.

            Returns: Response from the request.

        """
        url = urljoin(self.root_url, path)
        headers = self._get_headers()
        if extra_headers:
            headers.update(extra_headers)
        response = requests.put(url, json=data, headers=headers)
        response.raise_for_status()
        return response

    def delete(self, path: str) -> Response:
        """Makes a DELETE request to a REST API.

            Args:
                path: Endpoint to make the POST request to.

            Returns: Response from the request.

        """
        path = urljoin(self.root_url, path)
        response = requests.delete(path, headers=self._get_headers())
        response.raise_for_status()
        return response
