# Metrics Reporter

Service to collect various metrics from the services that make up a ZVI deployment
and report them to the Stackdriver Monitoring service. This makes these metrics
available for use in autoscaling, reporting, and monitoring.

## Dependencies

All dependencies are tracked in the applications Pipenv file.

### Install

1. `cd` into the `reporter` base directory (`zmlp/applications/reporter`).
1. Run `pipenv sync`.

### Add

1. Run `pipenv install $Name_Of_Library`
