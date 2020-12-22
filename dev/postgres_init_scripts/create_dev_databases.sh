#!/usr/bin/env bash

set -e
set -u

function create_user_and_database() {
	local database=$1
	local pw=$2
	echo "  Creating user and database '$database'"
	psql -v ON_ERROR_STOP=1 --username "admin" <<-EOSQL
	    CREATE USER $database WITH PASSWORD '$pw' SUPERUSER;
	    CREATE DATABASE $database;
	    GRANT ALL PRIVILEGES ON DATABASE $database TO $database;
EOSQL
}

create_user_and_database zorroa zorroa
create_user_and_database wallet a8fnnbe934j
create_user_and_database metrics 2mAPDWhuiYdIW69u
