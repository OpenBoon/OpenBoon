# Core plugin

This plugin contains the following:

## Generators:

#### FileUploadGenerator
A Generator for handling file uploads.
#### FileGenerator
A Generator that emits a static list of files.  This generator can be used
when you know the files you want to process.
#### FileSystemGenerator
Recursively walks the given file paths and consume all files.
#### AssetSearchGenerator
Generates frames based on an elastic search query.
## Collectors:

#### ExpandCollector
A collector which takes collected frames and a sub execute pipeline and
submits it back to the execution engine to be executed as a separate process.
#### ImportCollector
Collect import files and register them with the archivist.
## Processors:

#### GroupProcessor
A GroupProcessor is for holding sub processors. By itself, GroupProcessor is a no-op.
#### SetAttributesProcessor
SetAttributesProcessor accepts a map of attributes and sets them on the document.
#### PythonScriptProcessor
Execute a Python script.
#### AssertProcessor
Evaluates a Python expression and emits an error if
the expression evaluates to true.
#### ReturnResponseProcessor
Gathers up all assets and sends them back to
the archivist which forwards them onto waiting API calls.
#### DownloadAssetProcessor
Downloads a file and stores its location in the temp metadata.
#### DeleteAssetProcessor
Deletes an asset and skips all further processing of the document.
#### SetIdProcessor
Sets a new ID for an asset based on an existing attribute.
#### AddPermissionProcessor
Add a permission to the current asset.
