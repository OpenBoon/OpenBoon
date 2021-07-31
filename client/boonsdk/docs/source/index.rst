.. BoonSDK documentation master file, created by
   sphinx-quickstart on Wed May 19 10:27:00 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

BoonSDK Documentation
=====================

A Python library for BoonAI machine learning platform. It lets you do anything
the platform can do but from within Python rather than the UI.

Installation
------------

The latest stable version `is available on PyPI <https://pypi.python.org/pypi/boonsdk/>`_. Either add ``boonsdk`` to your ``requirements.txt`` file or install with pip::

       pip install boonsdk


Getting started
---------------

To talk to a BoonAI server, you first need to instantiate a client. You can use :py:func:`~boonsdk.app_from_env` to connect
to BoonAI if you've set a Base64 encoded BoonAI ApiKey as an environment variable.

.. code-block:: bash

    export BOONAI_APIKEY="<Base64 Encoded Key"

.. code-block:: python

  import boonsdk
  app = boonsdk.app_from_env()




You can load your key from a file.

.. code-block:: python

   app = boonsdk.app_from_keyfile("mykey.json")



You can load your JSON based BoonAI API key directly.

.. code-block:: python

   import boonsdk
   apikey = {
       "accessKey": "UT3rW1J68QCCkddiPuHOXg",
       "secretKey": "wueiEHo44hVK3H1nPkzh1g"
   }
   app = boonsdk.BoonApp(apikey)

You can now make calls into the BoonAI system.

For example you can ingest a directory of files and run AWS label detection.

.. code-block:: python

   app.assets.batch_upload_directory(
      "/path/to/my/images", modules=['aws-label-detection'])


Now that the files are uploaded and processed, lets see what we have.

.. code-block:: python

   app.analysis.get_prediction_counts('aws-label-detection', min_score=0.5)
   >> {
   >>    'cat': 15,
   >>    'dog': 29,
   >>    'rabbit': 42,
   >>    'mouse': 1
   >> }


We could then search for 'mouse' using the ElasticSearch API and print the file names
which contained a mouse.

.. code-block:: python

   search = {'query': { 'query_string': { 'query': 'mouse'}}}
   for asset in app.assets.search(search):
      asset.get_attr("source.path"))
   >> '/tests/pets/gad1123.jpg'


.. toctree::
  :hidden:
  :maxdepth: 2

  boon
  modules
  assets
  clips
  datasource
  fields
  datasets
  models
  jobs
  func
  webhooks
