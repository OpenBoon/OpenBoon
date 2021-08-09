Assets
======

Assets are the main currency of Boon AI. They typically contains the following data:

  * All of the standard metadata about uploaded files
  * Metadata generated from the various ML providers
  * Associated files generated during the importing process, which may include these:
     * Thumbnail images
     * Low-resolution version, high-resolution video
     * Raw API results from various ML providers
     * Web Video Text Tracks (WebVTT) files for speech transcription services


.. autoclass:: boonsdk.app.AssetApp
    :members:

.. autoclass:: boonsdk.Asset
    :members:

.. autoclass:: boonsdk.FileImport
    :members:

.. autoclass:: boonsdk.FileUpload
    :members:
