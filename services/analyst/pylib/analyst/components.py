import json
import logging
import os
import random
import shutil
import tempfile
import threading
import time
import uuid
import socket
import subprocess

from sys import platform

import psutil
import requests
from pathlib2 import Path

from .containerized import ContainerizedZpsExecutor

if platform == "darwin":
    from requests.packages.urllib3.exceptions import InsecureRequestWarning
    requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
else:
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


logger = logging.getLogger(__name__)

ZpsHeader = "######## BEGIN ########"
ZpsFooter = "######## END ##########"

__all__ = [
    "ApiComponents",
    "ClusterClient",
    "Executor",
    "ProcessorScanner",
    "get_sdk_version"
]


class ApiComponents(object):

    def __init__(self, args):
        self.client = ClusterClient(args.archivist, args.port)
        self.executor = Executor(self.client,
                                 ping_timer_seconds=args.ping,
                                 poll_timer_seconds=args.poll)


class ClusterClient(object):
    """
    The ArchivistClient is the client side implementation of the /cluster
    endpoit on the Archivist.  Communication to these endpoints are currently
    authorized via IP filter.  In the future this client may use JWT tokens.

    """
    def __init__(self, remote_url, my_port=8089):
        self.remote_url = remote_url or os.environ.get("ZORROA_ARCHIVIST_URL")
        self.headers = {
            "Content-Type": "application/json",
            "X-Analyst-Port": str(my_port)
        }
        try:
            hostname = socket.gethostname()
            self.headers["X-Analyst-Host"] = hostname
        except socket.gaierror as e:
            logger.warn("Unable to determine his machines hostname, %s" % e)

        logger.info("Remote URL: %s" % self.remote_url)

    def ping(self, ping):
        """
        Send a ping to the Archivist. The ping keeps the analyst record in the "Up"
        state.

        :param ping:
        :return:
        """
        return requests.post(self.remote_url + "/cluster/_ping",
                             verify=False, json=ping, headers=self.headers).json()

    def get_next_task(self):
        """
        Returns the highest priority task from the Archivist. The Task on the
        Archivist is set to the Queued state.
        :return:
        """
        data = {}
        try:
            rsp = requests.put(self.remote_url + "/cluster/_queue",
                               verify=False, json=data, headers=self.headers)
            if rsp.ok:
                return rsp.json()
        except requests.exceptions.ConnectionError as e:
            logger.warn("Connection error, failed to obtain next task %s, %s" %
                        (self.remote_url, e))
        return None

    def emit_event(self, task, etype, payload):
        """
        Emit an event to the Archivist.  Events are things like processing errors,
        expand requests, and started/stopped events.

        If the Archivist is down at the time an event is emitted, the function will
        hang and continually retry the event until the Archivist comes back online.

        Args:
            task (dict): A dict of Task properties.
            event (dict): An event

        """
        data = self.make_event(task, etype, payload)
        logger.info("POST %s/cluster/_event %s" % (self.remote_url, data))
        # Run forever until server comes back online
        backoff = 1
        while True:
            try:
                rsp = requests.post(self.remote_url + "/cluster/_event", verify=False,
                                    json=data, headers=self.headers)
                return rsp.status_code
            except requests.exceptions.ConnectionError:
                time.sleep(random.randint(1, min(60, backoff)))
                backoff *= 2

    def make_event(self, task, etype, payload):
        """
        Decorate an event with information about the running task.

        Args:
            etype (str): The event type.
            task (dict): A dictionary of task propertites.
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

        self.poll_count = 0
        self.current_task = None
        self.first_ping = True
        self.ping_timer = None
        self.poll_timer = None
        self.version = get_sdk_version()

        # Setup the ping timer thread.
        if self.ping_timer_seconds:
            self.start_ping_timer()

        # Setup the task poll timer thread.
        if self.poll_timer_seconds:
            self.start_poll_timer()

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
        except AttributeError as e1:
            logger.warning("Failed to kill task %s, current task was null, %s" % (task_id, e1))
        except Exception as e:
            logger.warning("Failed to kill task %s, was not current task: %s" % (task_id, e))
        return False

    def run_task(self, task):
        """
        Create and run a given task.

        :param task:
        :return:
        """
        self.current_task = ContainerizedZpsExecutor(task, self.client)
        try:
            # blocks until completed or killed
            return self.current_task.run()
        finally:
            self.current_task = None

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
            logger.info("Fetched next task: %s:" % task)
            return task
        return None

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
            except Exception as e:
                logger.warning("Failed to send ping %s" % e)

    def ___poll_timer_func(self):
        """
        Poll the Archivist for new tasks at regular interval.  This function
        will block forever and must be started from within a Thread.
        """
        # Don't poll for tasks until the first ping is handled
        # by the archivist.
        while True:
            time.sleep(self.poll_timer_seconds)
            if not self.first_ping:
                self.poll_count += 1
                if self.poll_count % 25 == 0:
                    logger.info("Polling Archivist for Task, count=%d" % self.poll_count)
                try:
                    while True:
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
        vf = os.environ.get("ZORROA_BUILD_FILE")
        if vf:
            with open(os.environ["ZORROA_BUILD_FILE"]) as fp:
                return fp.read().strip()
        else:
            raise IOError("ZORROA_VERSION_FILE env var not set")
    except IOError as e:
        logger.warning("Failed to open processors.json, %s" % e)
        return "unknown"


class ProcessorScanner(object):
    """Has methods for scanning for processors that the analyst can use."""
    def __init__(self, zpyfind_path='zpyfind'):
        self.zpyfind_path = zpyfind_path
        self.core_plugin_path = os.environ.get('ZORROA_CORE_PLUGIN_PATH',
                                               os.path.dirname(__file__) + "/../zplugins")
        self.ext_plugin_path = os.environ.get('ZORROA_EXT_PLUGIN_PATH', '/zorroa/plugins/local')
        self.gcs_plugin_path = os.environ.get('ZORROA_GCS_PLUGIN_PATH', '/zorroa/plugins/gcs')
        self.gcs_plugin_bucket = os.environ.get('GCS_EXT_PLUGIN_BUCKET')

    def get_processors(self):
        """
        Shell out to zpyfind and search specific python packages for zorroa
        processor sub-classes.

        Returns
            :obj:`list` of :obj:`str`: a list of detected search paths used.
        """
        search_paths = [self.core_plugin_path, self.ext_plugin_path, self.gcs_plugin_path]
        processors = None
        if not search_paths:
            logger.warn("No plugin search paths defined, skipping processors.json generation")
        else:
            cmd = [self.zpyfind_path]
            cmd.extend(search_paths)
            output = subprocess.check_output(cmd, shell=False)
            processors = json.loads(output)
        return processors

    def download_remote_processors(self):
        """Downloads processors from the configured remote location and places them in the
        python path.

        """
        bucket_path = self.gcs_plugin_bucket
        if bucket_path:
            destination_path = Path(self.gcs_plugin_path)
            destination_path.mkdir(exist_ok=True, parents=True)
            tmp_destination = destination_path.parent.joinpath('.zorroa_gcs_plugin_tmp')
            tmp_destination.mkdir(exist_ok=True, parents=True)
            subprocess.check_call(['gsutil', '-m', 'cp', '-r', bucket_path, str(tmp_destination)])
            bak_path = str(destination_path) + '.%s_bak' % uuid.uuid4()
            shutil.move(str(destination_path), bak_path)
            shutil.move(str(tmp_destination), str(destination_path))
            shutil.rmtree(bak_path)

    def scan_processors(self):
        """Downloads any new processors from the remote location and updates the
        processors registry file.

        """
        self.download_remote_processors()
        return self.get_processors()
