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

app_name = "archivist"

def main():
	base_dir = "%s-%s" % (app_name, get_version())
	cleanup(base_dir)
	shutil.copytree("resources", base_dir)

	os.mkdir("%s/lib" % base_dir)
	shutil.copy("../target/%s.jar" % app_name, "%s/lib" % base_dir)

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
