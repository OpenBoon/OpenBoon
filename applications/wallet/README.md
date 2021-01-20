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

### Option A. Using a runserver with the docker compose environment.
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

### Option B. Connecting to the Live Development Server

When you need to test against large datasets, importing them locally can take too
much time. The Development Server running in GCP has a number of example projects already
setup and it's possible to run your local Django code in a runserver that talks to the Dev
Server databases. 

There are specific requirements to do this, and caveats to keep in mind.

**Requirements:**
- You must have access to the `zvi-dev` project in the Google Cloud console.
- You have a basic understanding of the Kubernetes deployment in GCP (ask for help when/if needed).
- All the runserver command assume you're running in the pipenv/virtualenv you've setup for wallet.

**Caveats:**
- You'll be creating a settings file with sensitive keys and values - **_Do not_** check this into the
repository.
- If you're doing any backend work that involves migrations or changes to the database, **_you cannot
use this method_**. You local runserver could inadvertently break the database on the Development server.


#### Setup Steps
##### 1. Create New Django Settings file

You'll be creating a new Django settings file with the information your runserver needs to use 
the live Development servers database and ZMLP backend API. To do this:

1. In the `applications/wallet/app/wallet/settings` directory, you'll see a template 
   file `dev-server-template.py`. Copy this file into a new file named `dev-server.py`
   - Example: `cd applications/wallet/app/wallet/settings; cp dev-server-template.py dev-server.py`
1. This new `dev-server.py` file has been explicitly gitignored - do not check it in.    
1. There are 3 values you need to fill out in the new `dev-server.py` settings file - 
   `INCEPTION_KEY_B64`, the DB `PASSWORD`, and `EMAIL_HOST_PASSWORD`.
   In the GCP Web Console, go to the `zvi-dev` project, open the `wallet` K8s Workload, 
   and click `Get YAML` under the KUBECTL dropdown. Run the command
   that populates in your Cloud Shell, and you'll find the values you need in the 
   commands output. *Note:* The values in the yaml vary slightly from the names used in the
   settings file.
1. With the 3 settings filled out, your settings file is ready.

#### 2. Setup a Google Cloud SQL Proxy

Next, you'll need to set up a Google Cloud SQL Proxy. This is a tunnel that runs on your
machine, and will forward all requests to the standard Postgres port to the Postgres port
on the Live Development Servers Postgres instance in the cloud. 

1. Follow the [Quickstart](https://cloud.google.com/sql/docs/postgres/quickstart-proxy-test#macos-64-bit) for setting up a local Cloud SQL Proxy. You'll need access to the GCP Project to do this.

#### 3. Run Cloud SQL Proxy & Local Development Server

The last step is to run both the Cloud SQL Proxy and an instance of your Django runserver
(using the newly created `dev-server.py` settings file we previously created).

1. The Cloud SQL Proxy will forward all requests to `localhost:5432` to the Dev Server's Postgres
   Database. *NOTE*: Make sure you don't have anything already running and listening on that port,
   such as a local instance of Postgres running, or a local Docker Compose setup with running 
   containers.
1. Start the Cloud Sql Proxy. Your command should look similar to:
   `cloud_sql_proxy -instances=zvi-dev:us-central1:zmlp=tcp:5432`.
1. Start your runserver: `./manage.py runserver --settings='wallet.settings.dev-server'`. If you
   get an error about the port already being in use, make sure to stop any other runservers you
   may have running in the background.
1. Navigate to `localhost:8000` in your browser, you should get a 404 error page with a listing
   of the available endpoints - this means it's working! At this point, go to 
   `localhost:8000/admin`, login with the Dev Server Credentials admin credentials stored in 1Password,
   and then navigate to `localhost:8000/api/v1/projects` to get to the DRF Browsable API.
1. You now have a local runserver, running with any changes to your local codebase, but using the
live development servers data. (Keep that in mind before creating a large amount of projects/users/etc.)

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
