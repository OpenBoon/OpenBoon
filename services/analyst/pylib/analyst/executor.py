import json
import logging
import os
import threading
import time

import docker
import zmq

logger = logging.getLogger(__name__)

# The default container port
CONTAINER_PORT = 5001


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
        Create a new ContainerizedZpsExecutor.

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
        self.event_counts = {}

    def run(self):
        """
        Execute the full ZPS script.
        """
        self.client.emit_event(self.task, "started", {})
        try:
            if self.script.get("generate"):
                self.generate()
            if self.script.get("assets"):
                assets = self.process()
                if assets:
                    self.client.emit_event(
                        self.task, "index", {"assets": assets})
        except Exception as e:
            logger.warning("Failed to execute ZPS script, {}".format(e))
            self.exit_status = 1
        finally:
            # Emit a task stopped to the archivist.
            self.client.emit_event(self.task, "stopped", {
                "exitStatus": self.exit_status,
                "newState": self.new_state,
                "manualKill": self.killed
            })

        result = self.event_counts.copy()
        result["exit_status"] = self.exit_status
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
                    self.container = DockerContainerWrapper(self.client, self.task, proc["image"])
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
            self.exit_status = 1

    def process(self):
        """
        Iterate over the document processors and execute each one.
        """
        processors = self.script.get("execute", [])
        assets = self.script.get("assets", [])

        logger.info("running {} processors in execute, {} objects"
                    .format(len(processors), len(assets)))

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
                assets = self.run_containerized_processor(proc, assets)

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
            self.exit_status = 2

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
            self.container = DockerContainerWrapper(self.client, self.task, ref["image"])
            self.container.wait_for_container()

        if assets:
            for asset in assets:
                result = self.container.execute_processor(ref, asset)
                # Check to see if the object got skipped before adding
                # it to the result.
                if not result.get("skip"):
                    results.append(result["asset"])
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
            if self.stop_container(reason):
                self.new_state = new_state
                return True
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


class DockerContainerWrapper(object):
    """
    DockerContainerWrapper wraps a docker Container instance.

    Attributes:
        task (dict): The task we're executing.
        image: (str): the

    """
    def __init__(self, client, task, image):
        """

        Args:
            executor:
            task:
            image:
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
        self.socket = ctx.socket(zmq.DEALER)

    def _pull_image(self):
        """
        Attempt to pull the docker image if the version does not exist locally.  Return
        the docker Image instance or the image name if the ANALYST_DOCKER_PULL
        env var is set to "false"

        Returns: The docker Image record.
            docker.Image
        """
        if os.environ.get("ANALYST_DOCKER_PULL", "true") == "false":
            return self.image

        if ":" in self.image:
            full_name = self.image
        else:
            full_name = "{}:{}".format(self.image, os.environ.get("CLUSTER_TAG", "development"))

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

        volumes = {
            '/tmp': {'bind': '/tmp', 'mode': 'rw'}
        }

        network = self.get_network_id()
        if not network:
            ports = {'{}/tcp'.format(CONTAINER_PORT): CONTAINER_PORT}
        else:
            ports = None

        env = self.task.get("env", {})
        env.update({
            'PIXML_SERVER': os.environ.get("PIXML_SERVER")
        })

        logger.info("starting container {}".format(self.image))
        self.container = self.docker_client.containers.run(image, detach=True,
                                                           environment=env, volumes=volumes,
                                                           entrypoint="/usr/local/bin/server",
                                                           network=network,
                                                           ports=ports,
                                                           labels=["containerizer"])

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

        uri = "tcp://localhost:{}".format(CONTAINER_PORT)
        logger.info("Waiting for container '{}' on '{}' to come to life....".format(
            self.image, uri))

        self.socket.connect(uri)
        self.socket.send_json({"type": "ready", "payload": {}})

        # Give us 20 seconds to start.
        event = self.receive_event(20000)
        if event["type"] != "ok":
            raise RuntimeError(
                "Container {} in bad state, did not send ok event: {}".format(
                    self.image, event))
        logger.info("Container '{}' is ready to accept commands.".format(self.image))

    def __tail_container_logs(self):
        """
        Iterate and print the docker container log stream. This is run from
        within the log_thread.

        """
        logs = self.container.logs(stream=True)
        for line in logs:
            line = line.decode("utf-8").rstrip()
            logger.info("CONTAINER:%s" % line)

    def stop(self):
        """
        Stop the underlying docker Container.

        """
        if not self.killed and self.container:
            logger.info(
                "stopping container id='{}' image='{}'".format(self.container.id, self.image))
            self.container.kill(9)
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
                response = event["payload"]
            elif event_type == "finished":
                return response
            else:
                # Echo back to archivist.
                self.client.emit_event(self.task, event_type, event["payload"])

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
        timeout_time = int(time.time() * 1000) + (timeout or 0)
        while True:
            poll = self.socket.poll(timeout or 1000)
            if poll > 0:
                try:
                    event = self.socket.recv_json()
                except Exception as e:
                    logger.warning("event socket recv failed {} {}", type(e), e)
                    raise RuntimeError("ZMQ socket failure", e)

                self.log_event(event)
                if event["type"] == "hardfailure":
                    raise RuntimeError("Container failure, exiting event='{}'".format(event))
                if self.killed:
                    raise RuntimeError("Container was killed, exiting event='{}'".format(event))
                return event
            elif self.killed:
                raise RuntimeError("Container was killed")
            elif timeout and (time.time() * 1000) > timeout_time:
                return {"type": "timeout", "payload": {"timeout": timeout}}

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

        # Update the event counts, mainly for testing.
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
            with open("/proc/self/cgroup") as fp:
                return "container:{}".format(fp.readline().rstrip().split("/")[-1])
        except IOError:
            pass
        return None

    def check_killed(self):
        if self.killed:
            raise RuntimeError("Container was killed")
