#!/usr/bin/python
"""
A simple script for creating a tar that can be installed and
run on any mac/linux machine.

We could eventually move this to maven or add more options.
"""
import os
import tarfile
import subprocess
import shutil

def main():
	name = "archivist-%s" % get_version()
	cleanup(name)
	shutil.copytree("resources", name)

	os.mkdir("%s/lib" % name)
	shutil.copy("../target/archivist.jar", "%s/lib" % name)

	tar = tarfile.open("%s.tar.gz" % name, "w:gz")
	tar.add(name)
	tar.close()
	cleanup(name)

def cleanup(name):
	try:
		shutil.rmtree(name)
	except OSError, e:
		# Just ignore this if its not there
		pass

def get_version():
	cmd = ["java", "-cp", "../target/archivist.jar", "com.zorroa.archivist.Version"]
	output = subprocess.Popen(cmd, stdout=subprocess.PIPE).communicate()[0].strip()
	return output

if __name__ == '__main__':
	main()