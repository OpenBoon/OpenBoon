# Wallet

# Development Quickstart
_NOTE_: Zorroa issues Macbooks to developers so all instructions are for MacOS.

## Prerequisites
- Latest Docker and docker-compose installed.

## Start the local ZMLP Deployment

First off we will use docker-compose to start a local deployment of ZMLP. This will pull all of the 
latest deployed container images and start them up locally. Once you have a complete ZMLP
deployment running based off the main code branch, `development`. Once the deployment is up you 
can access wallet at http://localhost. There is a migration that creates a default superuser the first
time you start up the dev env, software@zorroa.com:admin.

### Steps

1. Run `docker-compose pull` - This will pull the latest images so you aren't working against old code.
1. Run `docker-compose up` - This will start up all of the services.

## Building & Running your local code.
Once you have made changes to the Wallet code you can build and run those changes in the local 
deployment. This will build all the local wallet code, package it in a docker container and then
run it. Once it's up and running you can test that your changes are working as you expect.

### Steps
1. Run `docker-compose pull` - You can skip this if you've done it recently. It's only needed when 
   code for other services has changed.
1. Run `docker-compose build wallet`
1. Run `docker-compose up` or `docker-compose up wallet` if you do not require the job processing queue.
This will reduce the load on your machine but you won't be able to import any assets.

--- 

# Advanced Development Options
There are multiple ways to run this application which may be better suited to the type of development
you are doing.

### Prerequisites
- [Python](https://www.python.org/downloads/) 3.8.0 or greater installed.
- Latest [Pipenv](https://github.com/pypa/pipenv) installed.
- [Homebrew](https://docs.brew.sh/Installation) installed.


#### Install 

Pipenv is used to manage package dependencies and the python version. Install it
with homebrew.

1. `pip install pipenv`
2. (Optional) Run `echo 'eval "$(pipenv --completion)"' >> ~/.bash_profile` to
   add pipenv completion to your shell.
3. Restart your shell to pickup the changes: `exec "$SHELL"`

#### Install Python dependencies

1. `cd` into the `wallet` base directory (`zmlp/applications/wallet`).
2. Run `pipenv install`

#### Use your Pipenv

To open a shell with your pipenv activated, run:

- `pipenv shell` If you're using an IDE with built-in Django support, here's
  some helpful commands for setting up the IDE:
- Get the path to your pipenv Python Interpreter: `pipenv --py`
- Get the location of the virtualenv that pipenv is using: `pipenv --venv`
- Install a new python package and add it to your pipenv:
  `pipenv install $PACKAGE`

### 1. Using a runserver with the docker compose environment.
This configuration assumes that the docker-compose environment is up and running. The steps
below will start a Django runserver that points to all of the docker-compose services
running on your machine. Using a runserver prevents you from having to rebuild the wallet
container on every code change.

More info on the Django runserver can be found
[here](https://docs.djangoproject.com/en/2.2/intro/tutorial01/#the-development-server).

1. Verify that you are in a pipenv shell. Instructions are above.
1. CD into the project directory: `cd app`
1. `./manage.py runserver --settings=wallet.settings.docker-compose`
1. Your server will now be running on `http://localhost:8000`

- _Note:_ You can drop the `--settings=wallet.settings.docker-compose` from the previous
  commands if you specify this in the `DJANGO_SETTINGS_MODULE` env variable. For
  example: `export DJANGO_SETTINGS_MODULE=wallet.settings.docker-compose`

### 2. Using Development Server

When you need to test against large datasets, importing them locally can take too
much time. The Development Server running in GCP has a number of example projects already
setup and it's possible to run your local Django code in a runserver that talks to the Dev
Server databases. The steps below assume that you're using PyCharm and have access to the
"zvi-dev" GCP project.

#### Setup a Pycharm Configuration

1. Add a new Pycharm Configuration - if you already have a Runserver configuration setup then
copying that is a quick way to get started. Make sure the following settings are set:
    * Script path: `Absolute path to your manage.py file`
    * Paramaters: `runserver`
    * Environment Variables:
        * `DJANGO_SETTINGS_MODULE: wallet.settings`
        * `ZMLP_API_URL: https://dev.api.zvi.zorroa.com`
        * `DEBUG: true`
        * `BROWSABLE: true`
        * `SUPERADMIN: true`
        * `INCEPTION_KEY_B64`: `Pull from the Dev Server's Wallet Service Yaml`
        * `PG_HOST: localhost`
        * `PG_PASSWORD`: `Pull from the Dev Server's Wallet Service Yaml`
        * `SMTP_PASSWORD`: `Pull from the Dev Server's Wallet Service Yaml`
        * `ENVIRONMENT: zvi-dev`
        * `FQDN: https://dev.console.zvi.zorroa.com`
    * Python Interpreter: `Your local environments Project Interpreter`
    * Working Directory: `path to wallet/app`

#### Setup a Google Cloud SQL Proxy

1. Follow the [Quickstart](https://cloud.google.com/sql/docs/postgres/quickstart-proxy-test#macos-64-bit) for setting up a local Cloud SQL Proxy.

#### Run/Debug a Development Server

1. The Cloud SQL Proxy will forward all requests to `localhost:5432` to the Dev Server's Postgres
    Database. *NOTE*: Make sure you don't have anything already running and listening on that port,
    such as a local instance of Postgres, or a local Docker Compose setup.
1. Start the Cloud Sql Proxy.
1. Start the new Pycharm Runserver Configuration.
1. Navigate to `localhost:8000` in your browser, and login using the Dev Server Credentials
   at the `api/v1/login` endpoint.

---

#### Browsable API

One of the benefits of using the Backend runserver is that it will setup a
browsable API you can access through your web browser. This provides an easy way
to see what endpoints are available, the supported methods, and the expected
arguments for them. To access this:

1.  With the backend server running.
2.  Navigate to http://127.0.0.1:8000/api/v1/
3.  This will drop you in the API Root.

From here, you should be able to follow the links on the available resources to
see what is available.

---

### Style Guide

Unless otherwise noted below this projects adheres to the
[pep8](https://www.python.org/dev/peps/pep-0008/) style guide.

_Exceptions and Extension to the Rules:_

- Docstrings follow the google python style. An excellent example can be found
  [here](https://sphinxcontrib-napoleon.readthedocs.io/en/latest/example_google.html).
- Max line width is 100 characters.
- Lambda functions are avoided at almost all costs. We ride with Guido on this
  one.
- String formatting always uses the `f'{VARIABLE}'` style.
- Variable names are never abbreviated, characters are cheap and readability is
  priceless. Yes: `project` No: `proj`.
- Any block of code that needs to be separated by newlines is preceded by a
  comment.

---

### Testing

- Unit tests are located in each of the app directories in a file named
  `tests.py`
- There is a suite of smoke tests that run against a live server. These tests
  use the REST api to accomplish the basic functionality of the application.
  These tests are located in `wallet.tests`.
- Code coverage must meet or exceed 98% in order to pass CI.

---

## Building and Running the Docker Container

The Dockerfile builds a container with the Django project that is capable of
running the backend web server as well as a celery worker for the processing
queue.

- _Build the container._ - `docker build -t wallet .`
- _Run the web server._ -
  `docker run -p 8080:8080 wallet sh /app/start-server.sh`

---

## External Services

### Continuous Integration

CI is handled by Gitlab and configured in the gitlab-ci.yml file. The config
file is documented and the best place to go to understand what happens during
the CI process.

### Error Tracking

This application is configured to send all errors to the Sentry service
(https://sentry.io/organizations/zorroa-eb/projects/). The errors can be viewed
in the wallet app in the #Zorroa organization. The configuration can be found in
the settings file.

### Mail Delivery

Emails is configured to be sent via SMTP by [MailGun](https://app.mailgun.com/).
The credentials to MailGun are stored in 1password. The SMTP_PASSWORD can found
by going to the MailGun console [here](https://app.mailgun.com/) and looking at
mg.zorroa.com under the domains menu.

---

## Deployment Options

Many of the configuration options can be set using environment variables. Below 
are the current options. These environment variables need to be set on the running
wallet container.

| Environment Variable | Effect |
| -------------------- | ------ |
| ENABLE_SENTRY | Enables Sentry error logging if "true". |
| ENVIRONMENT | Name of the environment wallet is running in (i.e. qa, staging). |
| ZMLP_API_URL | FQDN for the ZMLP REST API. |
| PG_HOST | Hostname of the Postgres DB server. |
| PG_PASSWORD | Password to be used by the wallet Postgres user. |
| SMTP_PASSWORD | Password for the MailGun SMTP server used for sending emails. |
| GOOGLE_OAUTH_CLIENT_ID | Client ID for Google OAuth used for Google sign in. |

There are also specific Feature Flags that can be set be either directly overriding their
value in the Django settings file, or by setting an environment variable (with any value).  

| Backend Feature Flags | Effect |
| --------------------- | ------ |
| USE_MODEL_IDS_FOR_LABEL_FILTERS | Returns a Models ID instead of a name in the field endpoints return for Labels. | 
