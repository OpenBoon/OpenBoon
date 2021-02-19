# Metrics Reporter

Service to collect various metrics from the services that make up a ZVI deployment
and report them to the GCP Monitoring service. This makes these metrics
available for use in autoscaling, reporting, and monitoring. 

This repo consists of the main script, `reporter`, and the `lib` python module. The `reporter` 
script runs an infinite loop that reports the metrics and is used as the entrypoint for 
the Docker container. The `lib` module contains the code for gathering the individual metrics.
To add a new metric all you need to do is create a new class that inherits from 
`lib.metrics.BaseMetric` and add it to the list of `REGISTERED_METRICS` in the `reporter` script.

Use the following environment variables to configure the `reporter` script.

| Env Var | Description |
| ------- | ----------- |
| PROJECT_ID | ID of the GCP project to publish metrics to. |
| COLLECTION_INTERVAL | Interval, in seconds, at which metrics are published. Default: 60 | 
| BOONAI_API_URL | FQDN of the ZMLP api. Default: https://dev.api.zvi.zorroa.com | 
| INCEPTION_KEY_B64 | Base64 encoded inception key for ZMLP. | 


## Development
### Dependencies
All dependencies are tracked in the applications Pipenv file.

#### Install
1. `cd` into the `reporter` base directory (`zmlp/applications/reporter`).
1. Run `pipenv sync`.

#### Add
1. Run `pipenv install $Name_Of_Library`


