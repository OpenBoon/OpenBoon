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


class ContainerizedZpsExecutor(object):
    """
    This class is responsible for iteration and interpretation of
    ZPS scripts along with plugin container life cycle.
    """

    def __init__(self, task, client):
        self.task = task
        self.client = client
        self.is_killed = False
        self.new_state = None
        self.container = None
        self.script = self.task.get("script", {})
        self.event_counts = {}

    def kill(self, reason="manually killed"):
        """
        Stop the execution of the task by closing the event socket
        and killing the container.

        Args:
            reason (str): Why the task is being killed.

        Returns:
            bool: True if the task was actually killed, False if not.

        """
        # TODO: need to figure out how to kill
        raise RuntimeError("Not Implemented")

    def run(self):
        """
        Execute the full ZPS script.
        """
        self.client.emit_event(self.task, "started", {})
        exit_status = 0
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
            exit_status = 1
        finally:
            # Emit a task stopped to the archivist.
            self.client.emit_event(self.task,  "stopped", {"exit_status": exit_status})

        result = self.event_counts.copy()
        result["exit_status"] = exit_status
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
                    self.container = DockerContainerProcess(self, self.task, proc["image"])
                    self.container.wait_for_container()

                self.container.execute_generator(proc, settings)

                # Tear down the processor, optionally keeping the container
                # if its needed again.
                self.teardown_processor(proc, keep_container)

        except Exception as e:
            # If any exceptions bubble out here, then the task is a hard failure
            # and the container is immediately stopped.
            logger.warning("Failed to execute script, unexpected {}".format(e))
            self.stop_container()

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
            logger.warning("Failed to execute script, unexpected {}".format(e))
            self.stop_container()

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
            self.stop_container()
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
            self.container = DockerContainerProcess(self, self.task, ref["image"])
            self.container.wait_for_container()

        if assets:
            for asset in assets:
                result = self.container.execute_processor(ref, asset)
                # Check to see if the object got skipped before adding
                # it to the result.
                if not result.get("skip"):
                    results.append(result["asset"])
        return results

    def stop_container(self):
        """
        Stops the current container instance and sets the
        container property to None.
        """
        # TODO: The container might not have started yet when this gets run.
        if not self.container:
            logger.warning("stop_container did not have a container instance to stop.")
            return

        for k, v in self.container.event_counts.items():
            if k in self.event_counts:
                self.event_counts[k] += v
            else:
                self.event_counts[k] = v

        self.container.stop()
        self.container = None


class DockerContainerProcess(object):
    """
    Wraps a docker Container object and communicates with the
    containerized ZPSD process.

    """
    def __init__(self, executor, task, image):
        self.task = task
        self.image = image
        self.client = executor.client
        self.zpsd_ready = False
        self.docker_client = docker.from_env()
        self.container = self.__setup_container()
        self.event_counts = {}

        # Sets up a thread which iterates the container logs.
        self.log_thread = threading.Thread(target=self.__dump_container_logs)
        self.log_thread.daemon = True
        self.log_thread.start()

        self.socket = self.__setup_zpsd_socket()

    def __setup_zpsd_socket(self):
        """
        Open and listen on a random TCP port for ZPSD to connect.

        Returns:
            socket, int: A tuple of the ZMQ socket and port/

        """
        ctx = zmq.Context()
        zsock = ctx.socket(zmq.DEALER)
        return zsock

    def __setup_container(self):
        """
        Sets up the docker container.

        Returns:
            Container: A running docker Container

        """
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

        name = "{}-{}".format(self.image.replace("/", "_"), int(time.time()))
        logger.info("starting container {}".format(name))
        return self.docker_client.containers.run(self.image, detach=True,
                                                 environment=env, volumes=volumes,
                                                 entrypoint="/usr/local/bin/server",
                                                 name=name,
                                                 network=network,
                                                 ports=ports,
                                                 labels=["containerizer"])

    def wait_for_container(self):
        """
        Block until  the container to sends a ready event. This lets us
        know the container is up and operational.

        """
        max_wait_time = 60 * 5 * 1000
        total_wait_time = 0

        uri = "tcp://localhost:{}".format(CONTAINER_PORT)
        logger.info("Waiting for container '{}' on '{}' to come to life....".format(
            self.image, uri))

        while True:
            self.socket.connect(uri)
            self.socket.send_json({"type": "ready", "payload": {}})
            poll = self.socket.poll(timeout=1000)
            if poll > 0:
                event = self.socket.recv_json()
                self.log_event(event)
                if event["type"] != "ok":
                    raise RuntimeError(
                        "Container {} in bad state, did not send ready event: {}".format(
                            self.image, event))
                break
            time.sleep(0.25)
            total_wait_time = total_wait_time + 1000
            if total_wait_time > max_wait_time:
                raise RuntimeError("Container {} in bad state, did not send ready event.".format(
                    self.image))

        logger.info("Container '{}' is ready to accept commands.".format(self.image))

    def __dump_container_logs(self):
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
        logger.info("stopping container id='{}' image='{}'".format(self.container.id, self.image))
        self.container.kill(9)

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
        rsp = self.socket.recv_json()

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
        response = None

        self.socket.send_json(request)
        while True:
            event = self.receive_event()
            # Once we find the resulting object, return it back ou
            # so it can be passed into the next processor.
            event_type = event["type"]
            if event_type == "asset":
                response = event["payload"]
            elif event_type == "finished":
                break
            else:
                # Echo back to archivist.
                self.client.emit_event(self.task, event_type, event["payload"])

        return response

    def receive_event(self):
        """
        Wait and receive events from ZPSD.   Blocks forever until
        and event is receieved.

        Returns:
            dict: the last event from ZPSD.
        """
        event = self.socket.recv_json()
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
        except KeyError:
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
        except Exception as e:
            logger.debug("'{}' not running in docker: {}".format(self.image, e))
        return None
