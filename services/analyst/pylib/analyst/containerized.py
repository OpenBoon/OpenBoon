import logging
import docker
import zmq
import threading

logger = logging.getLogger(__name__)

# TODO: move this into SDK.

class ContainerizedZpsExecutor(object):
    """
    This class is responsible for the plugin docker container life cycle
    as well as the interpretation and execution of ZPS scripts within a
    Docker container.

    """
    def __init__(self, task, client):
        self.task = task
        self.client = client
        self.is_killed = False
        self.is_started = False
        self.new_state = None
        self.container = None
        self.script = self.task.get("script", {})

    def kill(self, task_id, new_state="waiting", reason="manually killed"):
        """
        Stops execution of the task.

        Args:
            task_id (str): The task id to kill. This is for validation purposes.
            new_state (str): The new state of the task.
            reason (str): Why the task is being killed.

        Returns:
            (bool): True if the task was actually killed, False if not.

        """
        # TODO: handle thread issues
        # TODO: validate task id
        logger.info("killing %s, reason: %s" % (self.task["id"], reason))
        if not self.is_killed and self.container:
            self.container.kill()
            self.is_killed = True
            return True
        return False

    def run(self):
        """
        Execute the full ZPS script.

        Returns:
            (dict): A dictionary of runtime statistics.

        """
        # TODO: handle generate
        # TODO: general cleanup
        # TODO: alternate indexing endpoint

        exit_status = 0
        event_counts = {}

        def meld_event_counts(new_counts):
            """Adds new event counts to our event count totals."""
            for k, v in new_counts.items():
                if k in event_counts:
                    event_counts[k] += new_counts[k]
                else:
                    event_counts[k] = new_counts[k]

        self.client.emit_event(self.task, "started", {})
        try:
            objects = self.script.get("objects", self.script.get("over"))
            execute = self.script.get("execute", [])

            if objects:
                # Run the proc on all objects, then gather then up
                # Then do next proc
                for idx, proc in enumerate(execute):

                    # If the next processor requires the same container
                    # then set keep_container=True
                    keep_container = False
                    image = proc["image"]
                    if idx < len(execute) - 1 and execute[idx+1]["image"] == image:
                        keep_container = True

                    # Runs all objects through the processor, returning
                    # objects in their new state.
                    objects = self.run_processor_on_objects(proc, objects)

                    # Updates our runtime event counts
                    meld_event_counts(self.container.event_counts)

                    # Tear down the processor, optionally keeping the container
                    # if its needed again.
                    self.teardown_processor(proc, keep_container)

            # Emit all objects for indexing.
            self.client.emit_event(self.task, "index", {"objects": objects})

        except Exception as e:
            # If any exceptions bubble out here, then the task is a hard failure
            # and the container is immediately stopped.
            exit_status = 16
            logger.warning("Failed to execute script,", e)
            self.stop_container()
        finally:
            # Emit a task stopped to the archivist.
            self.client.emit_event(self.task,  "stopped", {"exit_status": exit_status})

        # Return the event counts back. These are mainly just for unit testing
        # and not returned back to Archivist.
        event_counts["exit_status"] = exit_status
        return event_counts

    def teardown_processor(self, ref, keep_container):
        """
        Run the teardown on a given processor.  The teardown
        is executed within a container.

        Args:
            ref (dict): The processor ref
            keep_container (bool): True if container should be kept alive.

        """
        logger.info("tearing down processor task='{}' ref='{}'".format(self.task, ref))
        self.container.run_teardown(ref)
        # Destroy container if we don't need it for next iteration
        if not keep_container:
            self.stop_container()

    def run_processor_on_objects(self, ref, objs):
        """
        Runs a given Processor reference on an list of objects.
        Args:
            ref (dict): The processor reference.
            objs: (list): A list of objects to iterate over.

        Returns:
            (list): Returns the objects in their processed state.

        """
        results = []
        if not self.container:
            self.container = DockerContainerProcess(self.task, ref["image"], self.client)

        for obj in objs:
            if self.is_killed:
                break
            result = self.container.execute_processor_on_object(ref, obj)
            # Check to see if the processor is skipped.
            if not result.get("skip"):
                results.append(result["object"])
        return results

    def stop_container(self):
        """
        Stops the current container instance and sets the
        container property to None.

        """
        if not self.container:
            logger.warning("stop_container did not have a container instance to stop.")
            return
        self.container.stop()
        self.container = None


class DockerContainerProcess(object):
    """
    Wraps a docker Container object and exposes methods for communicating
    to the zpsd instance running within the container.

    """
    def __init__(self, task, image, client):
        self.task = task
        self.image = image
        self.client = client
        self.docker_client = docker.from_env()
        self.container = self.__setup_container()
        self.socket = self.__setup_zpsd_socket()
        self.event_counts = {}

        # TODO: find a way to improve this
        # Sets up a thread which iterates the container logs.
        self.log_thread = threading.Thread(target=self.__iterate_logs)
        self.log_thread.daemon = True
        self.log_thread.start()

    def __setup_zpsd_socket(self):
        """
        Sets up he zmq zpsd socket.

        Returns:
            (socket): The ZMQ socket.

        """
        ctx = zmq.Context()
        socket = ctx.socket(zmq.PAIR)
        socket.connect("tcp://localhost:5557")
        return socket

    def __setup_container(self):
        """
        Sets up the docker container.

        Returns:
            (Container): A running docker Container

        """
        volumes = {'/tmp': {'bind': '/tmp', 'mode': "rw"}}
        ports = {"5557/tcp": 5557}

        logger.info("starting container {}".format(self.image))
        return self.docker_client.containers.run(self.image,
                                                 detach=True, volumes=volumes, ports=ports)

    def __iterate_logs(self):
        """
        Iterate and print the docker container log stream. This is run from
        within the log_thread.

        """
        logs = self.container.logs(stream=True)
        for line in logs:
            print("CONTAINER:%s" % line.decode("utf-8").strip())

    def stop(self):
        """
        Stop the underlying docker Container.

        """
        logger.info("stopping container id='{}' image='{}'".format(self.container.id, self.image))
        self.container.kill(9)

    def run_teardown(self, ref):
        """
        Run teardown on the given ProcessorRef and reset event counts.

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
        self.client.emit_event(self.task, rsp["type"], rsp["payload"])
        self.event_counts = {}

    def execute_processor_on_object(self, ref, obj=None):
        """
        Execute a given Processor ref on an object.  In the case of
        a Generator, obj may be None.

        Once zpsd is contacted with the execute command, this
        function blocks forever until the result is sent back
        or a 'hardfailure' event is emitted by the container.

        Args:
            ref (dict): The Processor ref.
            obj (dict): The object or possibly None.

        """
        request = {
            "type": "execute",
            "payload": {
                "ref": ref,
                "object": obj
            }
        }
        self.socket.send_json(request)

        while True:
            event = self.socket.recv_json()

            # Update the event counts, mainly for testing.
            key = event["type"] + "_events"
            c = self.event_counts.get(key, 0)
            self.event_counts[key] = c+1

            # Once we find the resulting object, return it back ou
            # so it can be passed into the next processor.
            event_type = event["type"]
            if event_type == "object":
                return event["payload"]
            # Send an exception up for a hard failure, which fails the
            # task and shuts down the container.
            elif event_type == "hardfailure":
                raise RuntimeError("Container failure, exiting {}".format(event))
            else:
                # Echo back to archivist.
                self.client.emit_event(self.task, event_type, event["payload"])

    def close(self):
        """
        Closes any open resources associated with this instance.

        """
        self.client.close()













