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
import argparse

app_name = "analyst"

def main():

	parser = argparse.ArgumentParser()
	parser.add_argument("--root", help="The base output directory.  Defaults to %s-<version> in the current directory." % app_name)
	parser.add_argument("--compress", help="Compress into a tar.gz file and remove the root directory")
	args = parser.parse_args()

	if args.root:
		base_dir = args.root
	else:
		base_dir = "%s-%s" % (app_name, get_version())

	cleanup(base_dir)
	shutil.copytree("%s/resources" % os.path.dirname(__file__), base_dir)

	os.mkdir("%s/lib" % base_dir)
	shutil.copy("%s/../target/%s.jar" % (os.path.dirname(__file__), app_name), "%s/lib" % base_dir)

	if args.compress:
		tar = tarfile.open("%s.tar.gz" % base_dir, "w:gz")
		tar.add(base_dir)
		tar.close()
		cleanup(base_dir)

def cleanup(base_dir):
	try:
		shutil.rmtree(base_dir)
	except OSError, e:
		# Just ignore this if its not there
		pass

def get_version():
	cmd = ["java", "-cp", "../target/%s.jar" % app_name, "com.zorroa.%s.Version" % app_name]
	output = subprocess.Popen(cmd, stdout=subprocess.PIPE).communicate()[0].strip()
	return output

if __name__ == '__main__':
	main()
