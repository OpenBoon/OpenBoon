# Metrics Service

This service exists to collect a record of all api-calls/processors that run on assets. As
each Processor runs, the call to add analysis to the asset will also create a record of
the addition in this service. Entries in the database are created when the Archivist Pub/Subs
messages with metrics information. The container for this service runs a listener subscribed
to the archivist metrics topic and stores the data for each message in the database. An 
endpoint exists to create billing reports from this data.

## Development & Testing

This is a Django project. It relies upon Postgres to run the correct Aggregation queries
necessary for reporting, and as such Postgres must be used for development and deployment.

Create a new Pipenv from the pipfile in this project, and use that environment for all
development.
