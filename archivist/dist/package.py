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

app_name = "archivist"

def main():

	parser = argparse.ArgumentParser()
	parser.add_argument("--root", help="The base output directory.  Defaults to %s-<version> in the current directory." % app_name)
	parser.add_argument("--rebuild", action="store_true", help="Rebuild the jar file");
	parser.add_argument("--compress", action="store_true", help="Compress into a tar.gz file and remove the root directory")

	args = parser.parse_args()

	jarFile = "%s/../target/%s.jar" % (os.path.dirname(__file__), app_name)
	if not os.path.exists(jarFile) or args.rebuild:
		subprocess.call(["mvn", "package", "-Dmaven.test.skip=true", "-f", "%s/../pom.xml" % os.path.dirname(__file__)], shell=False)

	if args.root:
		base_dir = args.root
	else:
		base_dir = "%s-%s" % (app_name, get_version())

	cleanup(base_dir)

        print ("copying data into: " + str(base_dir) + "/")
	shutil.copytree("%s/resources/config" % (os.path.dirname(__file__)), base_dir + "/config")
	shutil.copytree("%s/resources/bin" % os.path.dirname(__file__), base_dir + "/bin")
	shutil.copytree("%s/../web" % os.path.dirname(__file__), base_dir + "/web")

	os.mkdir("%s/plugins" % base_dir)
	os.mkdir("%s/lib" % base_dir)
	shutil.copy(jarFile, "%s/lib" % base_dir)

	if args.compress:
                print "compressing..."
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
        dir_path = os.path.dirname(os.path.realpath(__file__)) 

	cmd = ["java", "-cp", "../target/%s.jar" % app_name, "com.zorroa.%s.Version" % app_name]
	output = subprocess.Popen(cmd, stdout=subprocess.PIPE, cwd=dir_path).communicate()[0].strip()
	return output

if __name__ == '__main__':
	main()
