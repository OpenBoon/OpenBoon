const taskLogs = `
running 9 processors in execute, 25 objects
Exception during processing: 'DockerClient' object has no attribute 'login_view'
Traceback (most recent call last):
  File "/service/pylib/analyst/executor.py", line 167, in process
    assets = self.run_containerized_processor(proc, assets)
  File "/service/pylib/analyst/executor.py", line 224, in run_containerized_processor
    self.container.wait_for_container()
  File "/service/pylib/analyst/executor.py", line 433, in wait_for_container
    self.__start_container()
  File "/service/pylib/analyst/executor.py", line 383, in __start_container
    image = self._pull_image()
  File "/service/pylib/analyst/executor.py", line 357, in _pull_image
    self._docker_login()
  File "/service/pylib/analyst/executor.py", line 334, in _docker_login
    self.docker_client.login_view(dh_creds["username"],
  File "/usr/local/lib/python3.8/dist-packages/docker/client.py", line 205, in __getattr__
    raise AttributeError(' '.join(s))
AttributeError: 'DockerClient' object has no attribute 'login_view'
Stopping container, reason: Exception during processing: 'DockerClient' object has no attribute 'login_view'
`

export default taskLogs
