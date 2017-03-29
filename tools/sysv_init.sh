#!/bin/sh
set -e

### BEGIN INIT INFO
# Provides:           Archivist/Analyst
# Required-Start:
# Required-Stop:
# Default-Start:      2 3 4 5
# Default-Stop:       0 1 6
# Short-Description:  Create lightweight, portable, self-sufficient
#                     containers.
# Description:
#  A Generic start script for running either Zorroa's archivist or analyst
#  You must specifiy which you want
#  This uses the start-stop-daemon command
#  To install, copy this file to /etc/init.d
#              sudo update-rc.d <script> defaults
#              sudo update-rc.d <script> enable
### END INIT INFO

export PATH=/sbin:/bin:/usr/sbin:/usr/bin:/usr/local/sbin:/usr/local/bin

# specify archivist or analyst
# keep it lower case because it's used for paths below
BASE=%{application}

# the user we run the server as, and were the software is installed to
SERVER_RUN_USER=zorroa

# the base path to where the server software is located
BASE_PATH=/opt/zorroa/$BASE

# PATH to the executable
SERVER=$BASE_PATH/bin/$BASE

# This is the pidfile that our application generates when run with -d
SERVER_PIDFILE=$BASE_PATH/run/$BASE.pid

# by default LOG file is set in the application.properties
# if running the the server with a -d then this is ignored
SERVER_LOGFILE=$BASE_PATH/logs/$BASE.log

# -d will run the server in daemon mode
SERVER_OPTS="-d --pidfile=$SERVER_PIDFILE"

SERVER_DESC="Zorroa $BASE"
###  end configurable settings

# Get lsb functions
. /lib/lsb/init-functions


# Check if the server is present
if [ ! -x $SERVER ]; then
	log_failure_msg "$BASE not present or not executable"
	exit 1
fi


case "$1" in
	start)
	    ulimit -n 65536
		# we don't use --background if we pass -d above
		start-stop-daemon --start \
			--exec "$SERVER" \
			--pidfile "$SERVER_PIDFILE" \
			--user "$SERVER_RUN_USER" \
                        --chuid "$SERVER_RUN_USER" \
                        --group "$SERVER_RUN_USER" \
			-- \
				$SERVER_OPTS \
				>> "$SERVER_LOGFILE" 2>&1

		log_end_msg $?
		;;

	stop)
		log_begin_msg "Stopping $SERVER_DESC"
		start-stop-daemon --stop --pidfile "$SERVER_PIDFILE" --retry 10
		log_end_msg $?
		;;

	restart)
		server_pid=`cat "$SERVER_PIDFILE" 2>/dev/null`
		[ -n "$server_pid" ] \
			&& ps -p $server_pid > /dev/null 2>&1 \
			&& $0 stop
		$0 start
		;;

	force-reload)
		$0 restart
		;;

	status)
		status_of_proc -p "$SERVER_PIDFILE" "$SERVER" "$SERVER_DESC"
		;;

	*)
		echo "Usage: service $BASE {start|stop|restart|status}"
		exit 1
		;;
esac
