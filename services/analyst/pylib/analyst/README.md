# Analyst

## Configuring Environment Variables

- STACKDRIVER_LOGGING - If this exists then the Analyst will configure a log handler to 
send logs to the Google Cloud Platform (GCP) Stackdriver service. This is only advised when 
running on a GCP host.

## Stackdriver Logs
If the analyst has the STACKDRIVER_LOGGING env environment varibale set and the server is 
running in GCP then the stackdriver logs will be formatted for easy searching. Each log 
from the analyst will have the following metadata added (when it applies). You can easily 
create stackdriver queries based on these labels to quickly find logs for analyst jobs.

- `labels.analyst_organization_id`
- `labels.analyst_job_id`
- `labels.analyst_task_id`
- `labels.analyst_asset_id`
- `labels.analyst_file_name`
