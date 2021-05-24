Web Hooks
=========

Webhooks are messages sent from BoonAI to a web server hosted on your side
when something happens to an Asset. The payload of a webhook consists of the Asset metadata
and details about why the webhook was fired.

.. autoclass:: boonsdk.app.WebHookApp
    :members:

.. autoclass:: boonsdk.WebHook
    :members:

.. autoclass:: boonsdk.WebHookTrigger
    :members:

.. autofunction:: boonsdk.entity.webhook.validate_webhook_request_headers
