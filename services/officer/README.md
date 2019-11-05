# Officer

The Officer is responsible for rendering thumbnail and metadata dumps for PDF, XLS, DOC, and PPT files.

The Officer requires some type of shared storage where it write outputs.  By default each request gets it's
own output directory, or you can specify a specific sub directory with your request. All files are
cleaned up by the Officer after 12 hours.

To test locally:

`java -jar target/zdocextract.jar -p 7080 -s /shared_data/officer`

`curl -XPOST http://localhost:7080/extract -d '{"input_file": "gs://zorroa-dev-data/office/lighthouse.docx"}'`

## API

The most basic request for rendering proxies and metadata for all pages.

**Endpoint:**

POST /extract

**Request Body:**

```
{
    "input_file": "gs://zorroa-dev-data/office/lighthouse.docx",
    "content": true
}
```

**Response:**

The response will contain the full path to the output directory:

```
{
    "output" : "/tmp/officer/2019100913/3dd75f64-4b4e-4ea2-bc1e-49b55e8c93e7"
}
```

Within the output directory, there will be two sequences of files.  These files are
numbered by page.

```
metadata.1.json
metadata.2.json
metadata.3.json
proxy.1.jpg
proxy.2.jpg
proxy.3.jpg
```

## Processor

The Office Processor should utilize this service like so:

1. When processing the parent, render all the thumbnails and metadata files.
2. Save the output path in each child's tmp namespace.
3. When processing the child, look for proxy.$page.jpg and metadata.$page.json in the output path.

If the proxy or metadata files don't don't get created, there was likely and error for OOM, so
just don't add a children for that asset and throw a non-fatal exception that will get added
to the error system.


