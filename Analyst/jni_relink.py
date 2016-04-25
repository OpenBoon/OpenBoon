#!/usr/bin/env python

#
# Usage: jni_relink <library> [local_lib_dir]
#
# Recursively traverses the library dependencies using otool, copies any /usr/local
# libraries to target/jni and updates the linkage to use @loader_path so that we
# load our own library copies instead of relying on installing to /usr/local.
# We always check for a local copy of the library in <Ingestors>/lib before using
# the system library, which may be compiled for a specific target.
#

import os
import ntpath
from sets import Set
import shutil
import sys

bundle_lib_path = "@loader_path"
local_lib_dir = "target/jni"
rpath_dir = "lib"
processed_libs = Set()

def findLibOnRpath(basename):
    find_cmd = "find " + rpath_dir + " -name " + basename
    find = os.popen(find_cmd).readlines()
    if len(find) != 1:
        return 0
    find_lib = find[0].rstrip(' \t\n')
    return find_lib

def copyAndUpdateId(src_lib, local_lib):
    new_id = 0
    basename = ntpath.basename(local_lib)
    new_id = bundle_lib_path + "/" + basename
    if not os.path.isfile(local_lib):
        if os.path.isfile(src_lib):
            shutil.copyfile(src_lib, local_lib)
            os.chmod(local_lib, 0755)
            id_cmd = "install_name_tool -id " + new_id + " " + local_lib
            # print("@rpath: " + id_cmd)
            os.popen(id_cmd)
    if new_id == 0:
        print("WARNING: Cannot find local copy of " + basename + " -- may cause SIGILL errors at runtime on older machines")
    return new_id

def relink(lib):
    # print("Relinking: " + lib)
    dep_cmd = "otool -L " + lib + " | sed 's/(.*//g'"
    # print(dep_cmd)
    deps = os.popen(dep_cmd).readlines()
    # print("Found " + repr(len(deps)) + " dependencies in " + lib)
    # print(deps)
    for dep in deps:
        dep = dep.rstrip(' \t\n').strip(' \t')
        dep_basename = ntpath.basename(dep)
        local_lib = local_lib_dir + "/" + dep_basename
        # print(dep + " base: " + dep_basename + " local: " + local_lib)
        if dep.endswith(':') or dep_basename == ntpath.basename(lib):
            # print("Skipping current lib: " + dep)
            continue
        if dep.startswith('@rpath/') or dep.startswith('@loader_path') or dep.startswith('/usr/local/'):
            find_lib = findLibOnRpath(dep_basename)
            if find_lib == 0:
                print("Cannot find local library " + dep_basename)
                exit(-1)
            new_id = copyAndUpdateId(find_lib, local_lib)
            if dep.startswith('@rpath') or dep.startswith('/usr/local/'):
                change_cmd = "install_name_tool -change " + dep + " " + new_id + " " + lib
                # print("@rpath: " + change_cmd)
                os.popen(change_cmd)
                if not dep.startswith('/usr/local/'):
                    alt_id = dep
                    if os.path.isfile(dep):
                        # print("otool -DX " + dep)
                        alt_id = os.popen("otool -DX " + dep).readline().rstrip('\n')
                    change_cmd = "install_name_tool -change " + alt_id + " " + new_id + " " + lib
                    # print(change_cmd)
                    os.popen(change_cmd)
        else:
            # print("Skipping system dependency: " + dep)
            continue

        if (dep_basename not in processed_libs):
            processed_libs.add(dep_basename)
            relink(local_lib)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print "Usage: " + sys.argv[0] + " <library>"
        exit(-1);
    lib = sys.argv[1]
    relink(lib)
    lib_cmd = "install_name_tool -id " + bundle_lib_path + "/" + ntpath.basename(lib) + " " + lib
    # print("Top: " + lib_cmd)
    os.popen(lib_cmd)
    print("Relinked " + repr(len(processed_libs)) + " libraries from " + lib + " locally into " + local_lib_dir)