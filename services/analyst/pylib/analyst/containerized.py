import logging
import os
import socket
import subprocess
import threading
import time

import docker
import zmq

logger = logging.getLogger(__name__)


class ContainerizedZpsExecutor(object):
    """
    This class is responsible for iteration and interpretation of
    ZPS scripts along with plugin container life cycle.
    """
    def __init__(self, task, client):
        self.task = task
        self.client = client
        self.is_killed = False
        self.is_started = False
        self.new_state = None
        self.container = None
        self.script = self.task.get("script", {})
        self.event_counts = {}

        self.socket, self.port = self.__setup_zpsd_socket()

    def __setup_zpsd_socket(self):
        """
        Open and listen on a random TCP port for ZPSD to connect.

        Returns:
            socket, int: A tuple of the ZMQ socket and port/

        """
        ctx = zmq.Context()
        zsock = ctx.socket(zmq.PAIR)
        port = zsock.bind_to_random_port("tcp://*", min_port=2000, max_port=10000)
        return zsock, port

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
        file_types = self.script.get("settings", {}).get("fileTypes", [])

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
                self.container.execute_generator(proc, file_types)

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
                self.teardown_processor(proc, keep_container)
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
        self.socket = executor.socket
        self.port = executor.port
        self.zpsd_ready = False
        self.docker_client = docker.from_env()
        self.container = self.__setup_container()
        self.event_counts = {}

        # Sets up a thread which iterates the container logs.
        self.log_thread = threading.Thread(target=self.__iterate_logs)
        self.log_thread.daemon = True
        self.log_thread.start()

        # Wait for the container to fully initialized
        self.__wait_for_container()

    def __setup_container(self):
        """
        Sets up the docker container.

        Returns:
            Container: A running docker Container

        """
        network = os.environ.get("ZMLP_NETWORK", "dev_default")

        if in_container():
            host = socket.gethostbyname(socket.gethostname())
        else:
            host = "host.docker.internal"

        volumes = {
            '/tmp':  {'bind': '/tmp', 'mode': 'rw'}
        }

        env = self.task.get("env", {})
        env.update({
            'ZMLP_EVENT_HOST': 'tcp://{}:{}'.format(host, self.port),
            'PIXML_SERVER': os.environ.get("PIXML_SERVER")
        })

        logger.info("starting container {}".format(self.image))
        return self.docker_client.containers.run(self.image, detach=True,
                                                 environment=env, volumes=volumes,
                                                 entrypoint="/usr/local/bin/entrypoint",
                                                 network=network,
                                                 labels=["containerizer"])

    def __wait_for_container(self):
        """
        Block until  the container to sends a ready event. This lets us
        know the container is up and operational.

        """
        logger.info("Waiting for container to connect....")
        while True:
            # Timeout is in millis
            poll = self.socket.poll(timeout=5000)
            if poll == 1:
                event = self.socket.recv_json()
                if event["type"] != "ready":
                    raise RuntimeError("Container in bad state, did not send ready event")
                break
            time.sleep(0.25)
        logger.info("Container is ready to accept commands")

    def __iterate_logs(self):
        """
        Iterate and print the docker container log stream. This is run from
        within the log_thread.

        """
        logs = self.container.logs(stream=True)
        for line in logs:
            line = line.decode("utf-8").strip()
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

    def execute_generator(self, ref, file_types):
        """
        Execute a single generator with the file_types filter.

        Once zpsd is contacted with the execute command, this
        function blocks forever until the result is sent back
        or a 'hardfailure' event is emitted by the container.

        Args:
            ref (dict): The Processor reference.
            file_types (list): A list of file types

        """
        request = {
            "type": "generate",
            "payload": {
                "ref": ref,
                "file_types": file_types
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
        # Update the event counts, mainly for testing.
        key = event["type"] + "_events"
        c = self.event_counts.get(key, 0)
        self.event_counts[key] = c + 1

        if event["type"] == "hardfailure":
            raise RuntimeError("Container failure, exiting event='{}'".format(event))
        return event


def in_container():
    """
    Return true if we're running from within a docker container.

    Returns:
        bool: True if we're running from within a docker container.
    """
    try:
        out = subprocess.check_output('cat /proc/1/sched', shell=True)
        out = out.decode('utf-8').lower()
    except:
        return False

    checks = [
        'docker' in out,
        '/lxc/' in out,
        out.split()[0] not in ('systemd', 'init',),
        os.path.exists('/.dockerenv'),
        os.path.exists('/.dockerinit'),
        os.getenv('container', None) is not None
    ]
    return any(checks)





