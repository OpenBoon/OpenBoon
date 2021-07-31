Boon Functions
==============

Boon Functions allow you to deploy your own Python code into the BoonAI backend in
order to modify Assets before they are indexed.  The booksdk.func package contains
some necessary classes for returning results from your Boon Function as well as various
utility functions.


.. autofunction:: boonsdk.func.app_instance

.. autofunction:: boonsdk.func.get_proxy_image

.. autofunction:: boonsdk.func.get_proxy_record

.. autoclass:: boonsdk.func.FunctionResponse

.. autoclass:: boonsdk.func.Prediction

.. autoclass:: boonsdk.func.LabelDetectionAnalysis

.. autoclass:: boonsdk.func.ContentDetectionAnalysis
