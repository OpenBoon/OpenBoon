Data Sources
============

A DataSource describes a remote repository of files, most likely bucket storage such as S3/GCS and a
default set of AnalysisModules to apply on files imported from the DataSource.  BoonAI will crawl
the DataSource and import all file types that it supports.

.. autoclass:: boonsdk.app.DataSourceApp
    :members:

.. autoclass:: boonsdk.DataSource
    :members:
