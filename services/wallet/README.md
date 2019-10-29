# Wallet

## Developing
Requirements:
 - Python 3.8.0 or greater.
 - Node 6.10.1 or greater.
 - Latest docker & docker-compose installed.

### Development Server
The local development server is run using docker compose. The compose file will spin up a 
complete deployment locally. The containers will be built from your local development code. 
To start the environment or rebuild it after a code change just run the following commmand:

`docker-compose up --build -d`

Additionally the standard django runserver can be used for rapid development that doesn't
require a full deployment. More information is [here](https://docs.djangoproject.com/en/2.2/intro/tutorial01/#the-development-server).

### Style Guide
Unless otherwise noted below this projects adheres to the [pep8](https://www.python.org/dev/peps/pep-0008/)
style guide.

*Exceptions and Extension to the Rules:*
- Docstrings follow the google python style. An excellent example can be found 
[here](https://sphinxcontrib-napoleon.readthedocs.io/en/latest/example_google.html).
- Max line width is 100 characters.
- Lambda functions are avoided at almost all costs. We ride with Guido on this one.
- String formatting always uses the `f'{VARIABLE}'` style.
- Variable names are never abbreviated, characters are cheap and readability is priceless. 
Yes: `project` No: `proj`.
- Any block of code that needs to be separated by newlines is preceded by a comment.

### Testing
- Unit tests are located in each of the app directories in a file named `tests.py`
- There is a suite of smoke tests that run against a live server. These tests use the REST
api to accomplish the basic functionality of the application. These tests are located in 
`wallet.tests`.
- Code coverage must meet or exceed 98% in order to pass CI.

## Building and Running the Docker Container
The Dockerfile builds a container with the Django project that is capable of running the 
backend web server as well as a celery worker for the processing queue.

- *Build the container.*  - `docker build -t wallet .`
- *Run the web server.* - `docker run -p 8080:8080 wallet sh /app/start-server.sh`

## Continuous Integration
CI is handled by Gitlab and configured in the gitlab-ci.yml file. The config file is 
documented and the best place to go to understand what happens during the CI process.

## Error Tracking
This application is configured to send all errors to the Sentry service 
(https://sentry.io/organizations/zorroa-eb/projects/). The errors can be viewed in the 
wallet app in the #Zorroa organization. The configuration can be found in the 
settings file.
