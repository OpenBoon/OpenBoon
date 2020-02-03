# Wallet

## Developing

Requirements:

- Python 3.8.0 or greater.
- Node.js 12.14.0 or greater.
- Latest docker & docker-compose installed.
- Homebrew installed.

## Development Options

There are multiple ways to run this application and you should pick the best one
for your needs.

1. _Frontend Development_ - Use the Frontend development server. For the
   Backend, you can either use the runserver or the Docker container.
1. _Backend Development_ - Use the Backend Development server.
1. _Production-Like Testing_ - Use the Docker Compose setup. This will build the
   Frontend and setup the Backend to use a real Postgres DB, and setup an Nginx
   container to serve the static files.

---

### React Frontend Setup

The entire React project is in the `wallet/frontend` subdirectory.

See the [frontend README](frontend/README.md) for more info.

---

### Python Backend Setup

#### Install [Pipenv](https://github.com/pypa/pipenv)

Pipenv is used to manage package dependencies and the python version. Install it
with homebrew.

1. `brew install pipenv`
2. (Optional) Run `echo 'eval "$(pipenv --completion)"' >> ~/.bash_profile` to
   add pipenv completion to your shell.
3. Restart your shell to pickup the changes: `exec "$SHELL"`

#### Install Python dependencies

1. `cd` into the `wallet` base directory (`zmlp/applications/wallet`).
2. Run `pipenv sync`

#### Use your Pipenv

To open a shell with your pipenv activated, run:

- `pipenv shell` If you're using an IDE with built-in Django support, here's
  some helpful commands for setting up the IDE:
- Get the path to your pipenv Python Interpreter: `pipenv --py`
- Get the location of the virtualenv that pipenv is using: `pipenv --venv`
- Install a new python package and add it to your pipenv:
  `pipenv install $PACKAGE`

#### Start Backend Development Server

The Django runserver will serve out the frontend, assuming that a build is
present. More info on the Django runserver can be found
[here](https://docs.djangoproject.com/en/2.2/intro/tutorial01/#the-development-server).

1. CD into the project directory: `cd app`
1. Make sure you've built the Frontend if you expect the backend to serve it
   (instructions above).
1. Make sure your database is up to date:
   `./manage.py migrate --settings=wallet.settings.local`
   - If you receive an error about a Role, User, or DB not existing when running
     migrate, check the "Postgres Setup" section below.
1. `./manage.py runserver --settings=wallet.settings.local`
1. Your server will now be running on `http://localhost:8000`

- _Note:_ You can drop the `--settings=wallet.settings.local` from the previous
  commands if you specify this in the `DJANGO_SETTINGS_MODULE` env variable. For
  example: `export DJANGO_SETTINGS_MODULE=wallet.settings.local`

##### Postgres Setup

The development server has been setup to use Postgres for it's DB rather than
SQLite, due to us using some Postgres specific fields. The first time setting up
Postgres, you'll need to create the wallet DB and User/Role.

1. Make sure the `wallet` db has been created: `$ createdb wallet`
2. Start the PG Console: `$ psql -h localhost`
3. Create Role in the console (replace `$password` with password from settings
   file): `# CREATE ROLE wallet WITH LOGIN PASSWORD '$password';`
4. Set permissions in the console:
   `# GRANT ALL PRIVILEGES ON DATABASE wallet TO wallet;`
5. Give last permission to user in the console: `# ALTER USER wallet CREATEDB;`

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

### Docker Compose Environment

Use this to emulate a Production-like deployment. The Docker compose file will
spin up a complete deployment locally. The containers will be built from your
local development code. Instructions can be found in the [gitbook](https://app.gitbook.com/@zorroa/s/developers/guidelines/local-development)

Additionally the standard django runserver can be used for rapid development
that doesn't require a full deployment. More information is
[here](https://docs.djangoproject.com/en/2.2/intro/tutorial01/#the-development-server).
If you would like to use the runserver with a sqlite db so that you do not
depend on postgres running you use the `wallet.settings.local` settings file.
The full command for this is `./manage.py runserver --settings=`

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

### Testing

- Unit tests are located in each of the app directories in a file named
  `tests.py`
- There is a suite of smoke tests that run against a live server. These tests
  use the REST api to accomplish the basic functionality of the application.
  These tests are located in `wallet.tests`.
- Code coverage must meet or exceed 98% in order to pass CI.

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

### Deployment Options
Many of the configuration options can be set using environment variables. Below 
are the current options. These environment variables need to be set on the running
wallet container.

| Environment Variable | Effect |
| -------------------- | ------ |
| ENABLE_SENTRY | Enables Sentry error logging if "true". |
| ZMLP_API_URL | FQDN for the ZMLP REST API. |
| PG_HOST | Hostname of the Postgres DB server. |
| PG_PASSWORD | Password to be used by the wallet Postgres user. |
| SMTP_PASSWORD | Password for the MailGun SMTP server used for sending emails. |
| GOOGLE_OAUTH_CLIENT_ID | Client ID for Google OAuth used for Google sign in. |
| FRONTEND_SENTRY_DSN | DSN of the Sentry project to log frontend errors to. |
   
