import datetime
import logging
import os
import random
import socket
import tempfile
import threading
import time

import jwt
import psutil
import requests
import urllib3

from .executor import ZpsExecutor
from .cache import ModelCacheManager
from .logs import LogFileRotator

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

logger = logging.getLogger(__name__)

ZpsHeader = "######## BEGIN ########"
ZpsFooter = "######## END ##########"

__all__ = [
    "ServiceComponents",
    "ClusterClient",
    "Executor"
]


class ServiceComponents(object):

    def __init__(self, args):
        shared_key = None
        if args.credentials:
            with open(args.credentials) as fp:
                shared_key = fp.read()

        self.client = ClusterClient(args.archivist, shared_key, args.port)
        self.executor = Executor(self.client,
                                 ping_timer_seconds=args.ping,
                                 poll_timer_seconds=args.poll)


class ClusterClient(object):
    """
    The ClusterClient is the client side implementation of the /cluster
    endpoints on the Archivist.

    """

    def __init__(self, remote_url, shared_key, my_port=5000):
        self.remote_url = remote_url or os.environ.get("ZMLP_SERVER")
        self.shared_key = shared_key or os.environ.get("ANALYST_SHAREDKEY")
        self.version = get_sdk_version()

        if not self.remote_url:
            raise ValueError("No archivist URL has been set, try setting the ZMLP_SERVER env var")

        if not self.shared_key:
            raise ValueError("No shared key has been setting the ANALYST_SHAREDKEY env var")

        self.my_port = int(my_port)

        try:
            self.hostname = get_local_ip()
        except socket.gaierror as e:
            logger.warning("Unable to determine his machines hostname, %s" % e)

        logger.info("Remote URL: %s" % self.remote_url)

    def ping(self, ping):
        """
        Send a ping to the Archivist. The ping keeps the analyst record in the "Up"
        state.

        Args:
            ping (dict): The ping to send.
        """
        return requests.post(self.remote_url + "/cluster/_ping",
                             verify=False, json=ping, headers=self._headers()).json()

    def get_next_task(self):
        """
        Returns the highest priority task from the Archivist. The Task on the
        Archivist is set to the Queued state.
        """
        data = {}
        try:
            rsp = requests.put(self.remote_url + "/cluster/_queue",
                               verify=False, json=data, headers=self._headers())
            if rsp.ok:
                return rsp.json()
        except requests.exceptions.ConnectionError as e:
            logger.warning(
                "Connection error, failed to obtain next task %s, %s" % (self.remote_url, e))
        return None

    def emit_event(self, task, etype, payload):
        """
        Emit an event to the Archivist.  Events are things like processing errors,
        expand requests, and started/stopped events.

        If the Archivist is down at the time an event is emitted, the function will
        hang and continually retry the event until the Archivist comes back online.

        Args:
            etype (dict): A dict of Task properties.
            payload (dict): An event

        """
        data = self.make_event(task, etype, payload)
        logger.debug("POST %s/cluster/_event %s" % (self.remote_url, data))
        # Run forever until server comes back online
        backoff = 2
        while True:
            try:
                rsp = requests.post(self.remote_url + "/cluster/_event", verify=False,
                                    json=data, headers=self._headers())
                if rsp.status_code == 429:
                    logger.warning("Received backoff 429 from Archivist, waiting....")
                    raise RuntimeError("Received backoff from Archivist")
                else:
                    return rsp.status_code
            except (RuntimeError, requests.exceptions.ConnectionError):
                wait_time = random.randint(1, min(60, backoff))
                logger.warning("Sleeping {} seconds for Archivist to return....".format(wait_time))
                time.sleep(wait_time)
                backoff = backoff * 2

    def make_event(self, task, etype, payload):
        """
        Decorate an event with information about the running task.

        Args:
            etype (str): The event type.
            task (dict): A dictionary of task properties
            payload (dict): An event payload.

        Returns:
            dict: Task event properties.

        """
        new_event = {
            "type": etype.upper(),
            "taskId": task["id"],
            "jobId": task["jobId"],
            "payload": payload
        }
        return new_event

    def event_worker(self):
        while True:
            item = self.queue.get()
            try:
                self.emit_event(*item)
            except Exception as e:
                logger.warning("Failed to process queued event, %s" % e)
            finally:
                self.queue.task_done()

    def _headers(self):
        claims = {
            "aud": self.remote_url,
            "exp": datetime.datetime.utcnow() + datetime.timedelta(seconds=60),
            "port": self.my_port,
            "host": self.hostname,
            "version": self.version
        }
        token = jwt.encode(claims, self.shared_key, algorithm='HS256')

        headers = {
            "Content-Type": "application/json",
            "Authorization": "Bearer {}".format(token)
        }
        return headers


class Executor(object):
    """
    The Executor handles the scheduling, execution, and monitoring of a single task.

    Upon construction, the Executor will being polling the configured Archivist for Waiting
    tasks.  When a task is found, a ZpsGoProcessWrapper is created. The ZpsGoProcessWrapper
    shells out to ZpsGo, reads it's events from STDIN, and emits them back to the Archivist.

    """

    def __init__(self, client, ping_timer_seconds=0, poll_timer_seconds=0):
        """
        Create a new Executor.

        Args:
            client (:obj:`ClusterClient`): A ClusterClient instance for talking to Archivist.
            ping_timer_seconds (int): Ping timer interval in seconds.  0 to disable.
            poll_timer_seconds (int): Task polling timer interval in seconds.  0 to disable.

        """
        self.client = client
        self.ping_timer_seconds = ping_timer_seconds
        self.poll_timer_seconds = poll_timer_seconds

        self.disable_poll_timer = False
        self.poll_count = 0
        self.current_task = None
        self.previous_task = None
        self.first_ping = True
        self.ping_timer = None
        self.poll_timer = None
        self.version = get_sdk_version()
        self.log_rotator = LogFileRotator()

        # Setup the ping timer thread.
        if self.ping_timer_seconds:
            self.start_ping_timer()

        # Setup the task poll timer thread.
        if self.poll_timer_seconds:
            self.start_poll_timer()

        # Clear out model cache.
        ModelCacheManager.instance.clear_cache_root()

    def kill_task(self, task_id, new_state, reason):
        """
        Kill the current task if the task_id arg matches the current task id.

        Args:
            param task_id (str):  the task ID to kill
            param new_state (str): the new state the task should be in
            param reason (str): the reason the task was killed.

        Returns:
            bool: The return value. True for success, False otherwise.
        """
        try:
            return self.current_task.kill(task_id, new_state, reason)
        except Exception as e:
            logger.warning("Failed to kill task %s, %s" % (task_id, e))
        return False

    def run_task(self, task):
        """
        Create and run a the given task.

        Args:
            task (dict): A task dictionary.

        """
        # If a previous task was from another project, remove the model cache.
        if self.previous_task:
            if self.previous_task['projectId'] != task['projectId']:
                ModelCacheManager.remove_model_cache(self.previous_task)

        self.log_rotator.start_task_logging(task)
        self.current_task = ZpsExecutor(task, self.client)
        try:
            # blocks until completed or killed
            return self.current_task.run()
        finally:
            self.log_rotator.stop_task_logging()
            self.current_task = None
            self.previous_task = task

    def queue_next_task(self):
        """
        Continually poll for and execute tasks. Return if no task
        is found or a current task is already set.

        If a task is found, set the current_task property to a new
        ZpsGoProcessWrapper object, then run it. Continue to loop if tasks are found.

        :return:
        """
        if self.current_task:
            return None
        task = self.client.get_next_task()
        if task:
            logger.debug("Fetched next task: %s:" % task)
            return task
        return None

    def start_shutdown(self):
        """
        Disables the poll time and return true if the current task is None

        Returns:
            bool: True if there is no current task.
        """
        logger.info("Analyst shutting down, current task={}".format(self.current_task))
        self.disable_poll_timer = True
        if self.current_task:
            return {
                "task": self.current_task.task['taskId'],
                "name": self.current_task.task['name'],
                "exit": False
            }
        else:
            return {
                "task": None,
                "name": None,
                "exit": True
            }

    def send_ping(self):
        """
        Send a ping to the Archivist
        """
        bytes_to_megabytes = 1024 ** 2
        memstats = psutil.virtual_memory()
        data = {
            "version": self.version,
            "totalRamMb": memstats.total / bytes_to_megabytes,
            "freeRamMb": memstats.available / bytes_to_megabytes,
            "load": os.getloadavg()[0],
            "freeDiskMb": psutil.disk_usage(tempfile.gettempdir()).free / bytes_to_megabytes
        }

        # Add the current task if there is one.
        if self.current_task:
            data["taskId"] = self.current_task.task["id"]

        self.client.ping(data)
        return data

    def start_ping_timer(self):
        """
        Start a ping timer thread if one has not already been started.
        """
        if not self.ping_timer:
            logger.info("Setting up ping timer with '%d' second delay." %
                        self.ping_timer_seconds)
            self.ping_timer = threading.Thread(target=self.__ping_timer_func)
            self.ping_timer.daemon = True
            self.ping_timer.start()

    def start_poll_timer(self):
        """
        Start a task polling timer thread if one has not already been started.
        """
        if not self.poll_timer:
            logger.info("Setting up task poll timer with '%d' second delay." %
                        self.poll_timer_seconds)
            self.poll_timer = threading.Thread(target=self.___poll_timer_func)
            self.poll_timer.daemon = True
            self.poll_timer.start()

    def __ping_timer_func(self):
        """
        Send ping to Archivist at regular interval.  This function
        will block forever and must be started from within a Thread.
        """
        while True:
            time.sleep(self.ping_timer_seconds)
            try:
                self.send_ping()
                self.first_ping = False
            except Exception:
                logger.exception("Failed to send ping.")

    def ___poll_timer_func(self):
        """
        Poll the Archivist for new tasks at regular interval.  This function
        will block forever and must be started from within a Thread.
        """
        # Don't poll for tasks until the first ping is handled
        # by the archivist.
        while True:
            if self.disable_poll_timer:
                logger.info("terminating, shutdown by prestop")
                os._exit(0)
            time.sleep(self.poll_timer_seconds)
            if not self.first_ping:
                self.poll_count += 1
                if self.poll_count % 25 == 0:
                    logger.debug("Polling Archivist for Task, count=%d" % self.poll_count)
                try:
                    while True:
                        if self.disable_poll_timer:
                            break
                        task = self.queue_next_task()
                        if task:
                            self.run_task(task)
                        else:
                            break
                except Exception as e:
                    logger.warning("Failed to queue next task %s" % e)


def get_sdk_version():
    """
    Read the SDK version file and return the value. The path
    is provided by the ZORROA_VERSION_FILE environment variable.

    Returns (str): The version.
    """
    try:
        with open("BUILD", "r") as fp:
            return fp.read().strip()
    except IOError:
        return "unknown"


def get_local_ip():
    """
    Return the routable IP address.

    Returns:
        str: The IP address.
    """
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(('8.8.8.8', 1))  # connect() for UDP doesn't send packets
    return s.getsockname()[0]
