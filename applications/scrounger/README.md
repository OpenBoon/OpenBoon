# Scrounger


## Backend API Setup

### Prerequisites

This guide assumes you have the following already installed on your computer:

- [Python 3.7](https://www.python.org/downloads/release/python-379/)
- [Pipenv](https://pypi.org/project/pipenv/)

### Dependency Installation

We use Pipenv to handle package management for this project. Pipenv will create 
a "virtual environment" for the project and make sure all the requisite dependencies for
this project are installed there, rather than in your global python interpreter. To 
install the required dependencies:

1. In a terminal, make sure you are in the `application/scrounger` directory inside the
ZMLP repository.
1. Run: `pipenv install`

#### Pipenv - General Usage

- `pipenv -h` to display available commands.
- `pipenv install $package` to install the given `$package`
- `pipenv shell` to convert your current terminal shell to one using this project's virtual 
environment.

### Start Backend Runserver

Django provides a simple webserver for development. Starting the runserver will allow
you to hit the backend api on `localhost:8000` or similar (depending on the options you 
give).

1. If this is the first time you're running the runserver, or if the runserver mentions
"unapplied migrations" on startup, be sure to run:
    - `./manage.py migrate`

1. To start the runserver, run:
    - `./manage.py runserver`
    
- Note: The `manage.py` helper script lives inside the `applications/scrounger/api`
directory.

### Running the tests

We use `pytest` for running all of our tests. Assuming the dependencies from the pipenv
file have been installed, running the tests for the backend is as simple as:

1. `cd` into the `applications/scrounger/api` directory
1. Run: `pytest`

