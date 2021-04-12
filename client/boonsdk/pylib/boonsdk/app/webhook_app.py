from ..util import as_collection, as_id
from ..entity.webhook import WebHook


class WebHookApp:
    """
    An App instance for managing WebHooks.
    """
    def __init__(self, app):
        self.app = app

    def create_webhook(self, url, secret_key, triggers):
        """
        Create a new WebHook.  The WebHook will statt to function in
        between 1 and 5 seconds.

        Args:
            url (str): The HTTP/HTTPS endpoint for the webhook.
            secret_key (str): A secret key used to cryptographically sign the WebHook payload.
            triggers (list(WebHookTrigger)): A list of triggers the WebHook is interested in.

        Returns:
            WebHook: The newly created WebHook
        """
        body = {
            'url': url,
            'secretKey': secret_key,
            'triggers': [t.name for t in as_collection(triggers)]
        }
        return WebHook(self.app.client.post('/api/v3/webhooks', body))

    def pretest_webhook(self, url, secret_key, triggers):
        """
        Will send text data through the webhook system so your endpoint can be
        developed before the webhook is created.

        Args:
            url (str): The HTTP/HTTPS endpoint for the webhook.
            secret_key (str): A secret key used to cryptographically sign the WebHook payload.
            triggers (list(WebHookTrigger)): A list of triggers the WebHook is interested in.

        Returns:
            dict: A status dict.
        """
        body = {
            'url': url,
            'secretKey': secret_key,
            'triggers': [t.name for t in as_collection(triggers)]
        }
        return self.app.client.post('/api/v3/webhooks/_test', body)

    def test_webhook(self, webhook):
        """
        Sends tests data to the webhook endpoint.

        Args:
            webhook (str): A WebHook or it's unique ID.
        Returns:
            dict: A status dict
        """
        return self.app.client.post(f'/api/v3/webhooks/{as_id(webhook)}/_test')

    def delete_webhook(self, webhook):
        """
        Delete a webhook.

        Args:
            webhook (WebHook): A WebHook or it's unique ID.

        Returns:
            dict: A status dict
        """
        return self.app.client.delete(f'/api/v3/webhooks/{as_id(webhook)}')

    def find_webhooks(self, id=None, url=None, active=None, limit=None, sort=None):
        """
        Search for WebHook using some specific criterion.

        Args:
            id (str): The ID or list of IDs to search for.
            url (str): The URL or list of URLs to search for.
            active: (bool): If the WebHook is active or not.
            limit (int): The max number of results to return.
            sort (list):  A list of sort columns, for example ["name:asc", "type:desc"]

        Returns:
             generator: A generator which will return matching WebHooks when iterated.
        """
        body = {
            'urls': as_collection(url),
            'ids': as_collection(id),
            'sort': sort
        }
        if active:
            body['active'] = active
        return self.app.client.iter_paged_results('/api/v3/webhooks/_search', body, limit, WebHook)

    def get_webhook(self, id):
        """
        Get a WebHook by its unique ID.

        Args:
            id (str): The unique ID.

        Returns:
            WebHook: The WebHook
        """
        return WebHook(self.app.client.get(f'/api/v3/webhooks/{as_id(id)}'))

    def update_webhook(self, webhook, active=None, triggers=None, secret_key=None, url=None):
        """
        Update the given WebHook.

        Args:
            webhook (WebHook): A WebHook or it's unqiue ID.
            active (bool): If the WebHook is active or note.
            triggers (list): A list of triggers.
            secret_key (str): The secret key.
            url (url): The url.

        Returns:
            (dict): An update status
        """
        body = {
            'active': active,
            'triggers': triggers,
            'secretKey': secret_key,
            'url': url
        }
        return self.app.client.patch(f'/api/v3/webhooks/{as_id(webhook)}', body)
