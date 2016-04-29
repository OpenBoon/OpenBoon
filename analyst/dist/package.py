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
	parser.add_argument("--rebuild", action="store_true", help="Rebuild the jar file");
	parser.add_argument("--compress", action="store_true", help="Compress into a tar.gz file and remove the root directory")
	parser.add_argument("--config", default="local", help="The default configuration set, 'local' or 'enterprise')");

	args = parser.parse_args()

	jarFile = "%s/../target/%s.jar" % (os.path.dirname(__file__), app_name)
	if not os.path.exists(jarFile) or args.rebuild:
		subprocess.call(["mvn", "package", "-Dmaven.test.skip=true", "-f", "%s/../pom.xml" % os.path.dirname(__file__)], shell=False)

	if args.root:
		base_dir = args.root
	else:
		base_dir = "%s_%s-%s" % (app_name, args.config, get_version())

	cleanup(base_dir)
	shutil.copytree("%s/resources/config-%s" % (os.path.dirname(__file__), args.config), base_dir + "/config")
	shutil.copytree("%s/resources/bin" % os.path.dirname(__file__), base_dir + "/bin")

	os.mkdir("%s/plugins" % base_dir)

	os.mkdir("%s/lib" % base_dir)
	shutil.copy(jarFile, "%s/lib" % base_dir)

	if args.compress:
		tar = tarfile.open("%s.tar.gz" % base_dir, "w:gz")
		tar.add(base_dir)
		tar.close()
		cleanup(base_dir)

def cleanup(base_dir):
	try:
		print "removing " + base_dir
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
