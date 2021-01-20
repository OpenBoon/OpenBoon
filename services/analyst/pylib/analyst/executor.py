import json
import logging
import os
import random
import threading

import docker
import zmq

from .cache import TaskCacheManager, ModelCacheManager

logger = logging.getLogger('task')

# The default internal container port
CONTAINER_PORT = 5001

# The default local container port range
CONTAINER_PORT_RANGE = (20000, 60000)

# The hard failure exit status.
EXIT_STATUS_HARD_FAIL = 9

# The task has asset errors which in turn cause the task to fail.
EXIT_STATUS_HAS_ERRORS = 8


class ZpsExecutor(object):
    """
    This class is responsible for iteration and interpretation of
    ZPS scripts along with plugin container life cycle.

    Attributes:
        task (dict): The task tp execute.
        client (ClusterClient): A client for talking back to Archivist.
        killed (bool): If the script has been manually killed.
        new_state (string): A new task state set by a kill command.
        exit_status (int): The exit status of the task, >0 failure.
        container (DockerContainerWrapper): A docker container wrapper.
        script (dict): The script to execute
        event_counts (dict): Counters for event messages by type.
    """

    def __init__(self, task, client):
        """
        Create a new ZpsExecutor.

        Args:
            task (dict): A task.
            client (ClusterClient): An archivist client,.
        """
        self.task = task
        self.client = client
        self.killed = False
        self.new_state = None
        self.exit_status = 0
        self.container = None
        self.script = self.task.get("script", {})
        # A roll up of event counts from the various containers
        # that are spawned during processing.
        self.event_counts = {}

    def run(self):
        """
        Execute the full ZPS script.
        """
        settings = self.script.get("settings", {})
        self.client.emit_event(self.task, "started", {})
        try:
            if self.script.get("generate"):
                self.generate()
            if self.script.get("assets"):
                assets = self.process()
                if settings.get("index", False) and assets and self.exit_status == 0:
                    # Batch indexing expects map<id, document
                    payload = dict([(asset["id"], asset["document"]) for asset in assets])
                    self.client.emit_event(
                        self.task, "index", {
                            "assets": payload, "settings": self.script.get("settings", {})})
                    self.client.emit_event(self.task, "status", {
                        "status": "Indexing {} assets".format(len(assets))
                    })
        except Exception as e:
            logger.warning("Failed to execute ZPS script, {}".format(e))
            self.exit_status = EXIT_STATUS_HARD_FAIL
        finally:
            TaskCacheManager.remove_task_cache(self.task)

            # Emit a task stopped to the archivist
            exit_status = self.get_exit_status()
            self.client.emit_event(self.task, "stopped", {
                "exitStatus": exit_status,
                "newState": self.new_state,
                "manualKill": self.killed
            })

        result = self.event_counts.copy()
        result["exit_status"] = self.get_exit_status()
        return result

    def generate(self):
        """
        Iterate over the generators and execute each one.
        """
        generators = self.script.get("generate", [])
        settings = self.script.get("settings", {})

        logger.info("running {} processors in generate".format(len(generators)))

        try:
            for idx, proc in enumerate(generators):

                # If the next processor requires the same container
                # then set keep_container=True
                keep_container = False
                image = proc["image"]
                if idx < len(generators) - 1 and generators[idx + 1]["image"] == image:
                    keep_container = True

                # Run containerized generator
                if not self.container:
                    self.container = DockerContainerWrapper(
                        self.client, self.task, proc["image"])
                    self.container.wait_for_container()

                self.container.execute_generator(proc, settings)

                # Tear down the processor, optionally keeping the container
                # if its needed again.
                self.teardown_processor(proc, keep_container)

        except Exception as e:
            # If any exceptions bubble out here, then the task is a hard failure
            # and the container is immediately stopped.
            msg = "Exception during generation, reason: {}".format(e)
            logger.exception(msg)
            self.stop_container(msg)
            self.exit_status = EXIT_STATUS_HARD_FAIL

    def process(self):
        """
        Iterate over the document processors and execute each one.
        """
        processors = self.script.get("execute", [])
        assets = self.script.get("assets", [])

        logger.info("running {} processors in execute, {} objects"
                    .format(len(processors), len(assets)))

        total_processors = len(processors)

        try:
            for idx, proc in enumerate(processors):

                # If the next processor requires the same container
                # then set keep_container=True
                keep_container = False
                image = proc["image"]
                if idx < len(processors) - 1 and processors[idx + 1]["image"] == image:
                    keep_container = True

                # Runs all objects through the processor, returning
                # objects in their new state.
                self.client.emit_event(self.task, "status", {
                    "status": "Running: {}".format(proc["className"])
                })

                assets = self.run_containerized_processor(proc, assets)

                self.client.emit_event(self.task, "progress", {
                    "progress": int((idx + 1) / float(total_processors) * 100)
                })

                # Tear down the processor, optionally keeping the container
                # if its needed again.
                self.teardown_processor(proc, len(assets) and keep_container)

                if not assets:
                    logger.warning("All assets have been skipped")
                    break

        except Exception as e:
            # If any exceptions bubble out here, then the task is a hard failure
            # and the container is immediately stopped.
            msg = "Exception during processing: {}".format(e)
            logger.exception(msg)
            self.stop_container(msg)
            self.exit_status = EXIT_STATUS_HARD_FAIL

        return assets

    def teardown_processor(self, ref, keep_container):
        """
        Run the teardown on a given processor.

        Args:
            ref (dict): The processor reference.
            keep_container (bool): True if container should be kept alive.

        """
        logger.info("tearing down processor ref='{}'".format(ref["className"]))
        self.container.run_teardown(ref)
        # Destroy container if we don't need it for next iteration
        if not keep_container:
            self.stop_container("switching containers")
        else:
            logger.info("keeping container {}".format(ref["image"]))

    def run_containerized_processor(self, ref, assets):
        """
        Runs a given DocumentProcessor on a list of objects.

        Args:
            ref (dict): The processor reference.
            assets: (list): A list of asset Ids to iterate over.

        Returns:
            list: Returns the objects in their processed state.

        """
        results = []
        if not self.container:
            self.container = DockerContainerWrapper(
                self.client, self.task, ref["image"])
            self.container.wait_for_container()

        if assets:
            results = self.container.execute_processor_on_assets(ref, assets)
        return results

    def kill(self, task_id, new_state, reason="manually killed"):
        """
        Stop the execution of the task by closing the event socket
        and killing the container.

        Args:
            task_id (str): The task id.
            new_state: (str): The new state of the task once the kill occurs.
            reason (str): Why the task is being killed.

        Returns:
            bool: True if the task was actually killed, False if not.

        """
        if self.task["id"] == task_id:
            logger.info("killing task id: {}".format(task_id))
            if self.stop_container(reason):
                self.new_state = new_state
                return True
        logger.warning("Not killing task {}!={}".format(self.task["id"], task_id))
        return False

    def stop_container(self, reason):
        """
        Stops the current container instance and sets the
        container property to None.
        """
        if not self.container:
            logger.warning("stop_container did not have a container instance to stop.")
            return False

        logger.warning("Stopping container, reason: {}".format(reason))
        for k, v in self.container.event_counts.items():
            if k in self.event_counts:
                self.event_counts[k] += v
            else:
                self.event_counts[k] = v

        killed = self.container.stop()
        if killed:
            self.container = None
        return killed

    def get_exit_status(self):
        """
        Return the final exit status of the task.

        If there are hard failures like missing processors or containers, then then a hard
        failure status is set and an error is generated for all assets in the batch.
        If there are asset errors, then the task also fails.

        Returns:
            int: The exit status/
        """
        if self.event_counts.get("hardfailure_events", 0) > 0:
            return EXIT_STATUS_HARD_FAIL
        elif self.event_counts.get("error_events", 0) > 0:
            return EXIT_STATUS_HAS_ERRORS
        else:
            return self.exit_status


class DockerContainerWrapper(object):
    """
    DockerContainerWrapper wraps a docker Container instance.

    Attributes:
        task (dict): The task we're executing.
        image: (str): the

    """

    def __init__(self, client, task, image):
        """
        Create a new DockerContainerWrapper which manages the container process life cycle.

        Args:
            client (ClusterClient): A client for talking back to Archivist.
            task (dict): A task description.
            image (str): The docker image to run.
        """
        self.task = task
        self.image = image
        self.client = client
        self.docker_client = docker.from_env()
        self.event_counts = {}
        self.killed = False

        self.container = None
        self.log_thread = None

        ctx = zmq.Context()
        self.socket = ctx.socket(zmq.PAIR)
        self.port = random.randint(*CONTAINER_PORT_RANGE)

    def _docker_login(self):
        creds_file = os.environ.get("ANALYST_DOCKER_CREDS_FILE",
                                    "/etc/docker/.dockerconfigjson")
        try:
            with open(creds_file, "r") as fp:
                creds = json.load(fp)

            reg = "https://index.docker.io/v1/"
            dh_creds = creds["auths"][reg]
            self.docker_client.login(dh_creds["username"],
                                     dh_creds["password"],
                                     dh_creds["email"],
                                     reg)

        except FileNotFoundError:
            logger.warning("No docker creds file found.")
        except docker.errors.APIError as e:
            logger.error("Unable to log into docker-hub: {}".format(e))

    def _pull_image(self):
        """
        Attempt to pull the docker image if the version does not exist locally.  Return
        the docker Image instance or the image name if the ANALYST_DOCKER_PULL
        env var is set to "false"

        Returns: The docker Image record.
            docker.Image
        """
        if os.environ.get("ANALYST_DOCKER_PULL", "true") == "false":
            logger.info("Skipping docker pull, ANALYST_DOCKER_PULL==false")
            return self.image

        self._docker_login()

        if ":" in self.image:
            full_name = self.image
        else:
            full_name = "{}:{}".format(self.image, os.environ.get("CLUSTER_TAG", "latest"))

        logger.info('Checking for new image: {}'.format(full_name))
        try:
            remote_img = self.docker_client.images.pull(full_name)
            logger.info("loaded new image {} id={}".format(full_name, remote_img.id))
        except docker.errors.APIError:
            # Image wasn't found in remote repo.
            pass

        return full_name

    def __start_container(self):
        """
        Sets up the docker container.

        Returns:
            Container: A running docker Container

        """
        self.check_killed()
        image = self._pull_image()

        task_cache = TaskCacheManager.create_task_cache(self.task)
        model_cache = ModelCacheManager.create_model_cache(self.task)

        volumes = {
            "/tmp": {"bind": os.environ.get("ANALYST_TEMP", "/tmp"), "mode": "rw"}
        }

        network = self.get_network_id()
        logger.info("Docker network ID: {}".format(network))

        if not network:
            ports = {"{}/tcp".format(CONTAINER_PORT): self.port}
        else:
            ports = None

        env = self.task.get("env", {})
        env.update({
            "ZMLP_BILLING_METRICS_SERVICE":
                os.environ.get("ZMLP_BILLING_METRICS_SERVICE", "http://10.3.240.109"),
            "ZVI_MODEL_CACHE": model_cache,
            "TMPDIR": task_cache,
            "ZMLP_SERVER": os.environ.get("ZMLP_SERVER"),
            "OFFICER_URL": os.environ.get("OFFICER_URL"),
            # Get threads from task env, or os env.
            "ANALYST_THREADS": env.get("ANALYST_THREADS", os.environ.get("ANALYST_THREADS"))
        })

        logger.info("starting container {} vols={} network={} ports={}".format(
            self.image, volumes, network, ports))
        command = ["/usr/local/bin/server", "-p", str(self.port)]
        self.container = self.docker_client.containers.run(image, detach=True,
                                                           environment=env, volumes=volumes,
                                                           entrypoint=command,
                                                           network=network,
                                                           ports=ports,
                                                           labels=["zmlpcd"])

        logger.info("started container {} tags: {}".format(
            self.container.image.id, self.container.image.tags))

        # Sets up a thread which iterates the container logs.
        self.log_thread = threading.Thread(target=self.__tail_container_logs)
        self.log_thread.daemon = True
        self.log_thread.start()

    def wait_for_container(self):
        """
        Start container and block until the process completes.
        """
        self.check_killed()
        self.__start_container()

        uri = "tcp://localhost:{}".format(self.port)
        logger.info("Waiting for container '{}' on '{}' to come to life....".format(
            self.image, uri))

        retry = True
        while True:
            self.socket.connect(uri)
            self.socket.send_json({"type": "ready", "payload": {}})
            event = self.receive_event(20000)
            if event["type"] == "timeout":
                if retry:
                    logger.info("Retrying ready event")
                    retry = False
                    continue
                raise RuntimeError(
                    "Container {} in bad state, timed out".format(self.image))
            elif event["type"] == "ok":
                logger.info("Container '{}' is ready to accept commands.".format(self.image))
                return
            else:
                raise RuntimeError(
                    "Container {} in bad state, did not send ok event: {}".format(
                        self.image, event))

    def __tail_container_logs(self):
        """
        Iterate and print the docker container log stream. This is run from
        within the log_thread.

        """
        logs = self.container.logs(stream=True)
        for line in logs:
            if self.killed:
                return
            line = line.decode("utf-8").rstrip()
            logger.info("CONTAINER:%s" % line)

    def stop(self):
        """
        Stop the underlying docker Container. This can happen from an exception
        or manually.

        """
        if not self.killed and self.container:
            logger.info(
                "stopping container id='{}' image='{}'".format(self.container.id, self.image))
            self.container.kill()
            self.killed = True
            return True
        return False

    def run_teardown(self, ref):
        """
        Run teardown on the given ProcessorRef.

        Args:
            ref (dict): the Processor ref.

        """
        request = {
            "type": "teardown",
            "payload": {
                "ref": ref
            }
        }
        self.socket.send_json(request)
        rsp = self.receive_event(10000)

        # Emit the teardown event.
        self.client.emit_event(self.task, rsp["type"], rsp["payload"])

    def execute_generator(self, ref, settings):
        """
        Execute a single generator with the file_types filter.

        Once zpsd is contacted with the execute command, this
        function blocks forever until the result is sent back
        or a 'hardfailure' event is emitted by the container.

        Args:
            ref (dict): The Processor reference.
            settings (list): A dictionary of global script settings.

        """
        self.check_killed()
        request = {
            "type": "generate",
            "payload": {
                "ref": ref,
                "settings": settings
            }
        }

        self.client.emit_event(self.task, "status", {
            "status": "Running generator: {}".format(ref["className"])
        })

        self.socket.send_json(request)
        while True:
            event = self.receive_event()
            event_type = event["type"]
            if event_type == "finished":
                break
            else:
                self.client.emit_event(self.task, event_type, event["payload"])

    def execute_processor(self, ref, asset):
        """
        Execute a given Processor ref on an object.  In the case of
        a Generator, obj may be None.

        Once zpsd is contacted with the execute command, this
        function blocks forever until the result is sent back
        or a 'hardfailure' event is emitted by the container.

        Args:
            ref (dict): The Processor reference.
            asset (dict): The asset or possibly None.

        """
        request = {
            "type": "execute",
            "payload": {
                "ref": ref,
                "asset": asset
            }
        }
        self.check_killed()
        self.socket.send_json(request)
        while True:
            event = self.receive_event()
            event_type = event["type"]
            if event_type == "asset":
                return event["payload"]
            else:
                if event_type == "error":
                    logger.warning("processing error {}".format(event["payload"]))
                # Echo back to archivist.
                self.client.emit_event(self.task, event_type, event["payload"])

    def execute_processor_on_assets(self, ref, assets):
        """
        Execute a given Processor ref on an object.  In the case of
        a Generator, obj may be None.

        Once zpsd is contacted with the execute command, this
        function blocks forever until the result is sent back
        or a 'hardfailure' event is emitted by the container.

        Args:
            ref (dict): The Processor reference.
            assets (list): The asset or possibly None.

        """
        request = {
            "type": "execute",
            "payload": {
                "ref": ref,
                "assets": assets
            }
        }

        result = []
        result_counter = 0

        logger.info("processing {} assets".format(len(assets)))

        self.check_killed()
        self.socket.send_json(request)
        while True:
            event = self.receive_event()
            event_type = event["type"]
            if event_type == "asset":
                result_counter += 1
                if not event["payload"]["skip"]:
                    result.append(event["payload"]["asset"])
                if result_counter == len(assets):
                    logger.info("All assets processed")
                    break
            else:
                # Echo back to archivist.
                self.client.emit_event(self.task, event_type, event["payload"])

        return result

    def receive_event(self, timeout=None):
        """
        Wait and receive events from ZPSD.   Blocks forever until
        and event is received or the socket is closed.

        Args:
            timeout (int): The amount of time to wait for an event to
                happen, None for infinite time.

        Returns:
            dict: an event dict
        """
        wait_time = timeout
        while True:
            event = None
            # We have to use polling in all cases or else killing
            # the container depends up in a deadlocked socket.
            poll = self.socket.poll(500)
            if poll > 0:
                event = self.socket.recv_json()
            elif timeout:
                wait_time -= 500
                if wait_time <= 0:
                    event = {"type": "timeout", "payload": {"timeout": timeout}}

            if self.killed:
                raise RuntimeError("Container was killed, exiting event='{}'".format(event))

            if event:
                self.log_event(event)
                if event["type"] == "hardfailure":
                    raise RuntimeError("Container failure, exiting event='{}'".format(event))
                return event

    def log_event(self, event):
        """
        Log the given event structure.

        Args:
            event (dict): An event sent from ZMQ.
        """
        try:
            asset_id = event["payload"]["asset"]["id"]
        except (KeyError, TypeError):
            asset_id = None

        logger.info('Analyst received event =\'{}\' from image=\'{}\' assetId=\'{}\''.format(
            event["type"], self.image, asset_id))
        if logger.isEnabledFor(logging.DEBUG):
            logger.debug('------------------------------------------------------')
            logger.debug(json.dumps(event, indent=4))
            logger.debug('------------------------------------------------------')

        # Update the event counts
        key = event["type"] + "_events"
        c = self.event_counts.get(key, 0)
        self.event_counts[key] = c + 1

    def get_network_id(self):
        """
        Return the current containers network Id.  Plugin containers are given the same ID
        so they act like a form of side car container.

        Returns:
            str: The network Id.
        """
        try:
            with open("/proc/self/cpuset") as fp:
                return "container:{}".format(os.path.basename(fp.readline().rstrip()))
        except IOError:
            pass
        return None

    def check_killed(self):
        if self.killed:
            raise RuntimeError("Container was killed")
