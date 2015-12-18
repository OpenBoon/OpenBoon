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

def relink(lib):
    # print("Relinking: " + lib)
    dep_cmd = "otool -L " + lib + " | sed 's/(.*//g'"
    # print(dep_cmd)
    deps = os.popen(dep_cmd).readlines()
    # print("Found " + repr(len(deps)) + " dependencies in " + lib)
    # print(deps)
    for dep in deps:
        dep = dep.rstrip(' \t\n').strip(' \t')
        if dep.endswith(':'):
            # print("Skipping current lib: " + dep)
            continue
        if dep.startswith('@rpath/'):
            rpath_basename = ntpath.basename(dep)
            find_cmd = "find " + rpath_dir + " -name " + rpath_basename
            find = os.popen(find_cmd).readlines()
            if len(find) != 1:
                print("Error finding " + dep + ": Found " + find)
                exit(-1);
            find_lib = find[0].rstrip(' \t\n')
            find_local_lib = local_lib_dir + "/" + rpath_basename
            if not os.path.isfile(find_local_lib):
                if os.path.isfile(find_lib):
                    shutil.copyfile(find_lib, find_local_lib)
                    os.chmod(find_local_lib, 0755)
                    new_find_id = bundle_lib_path + "/" + rpath_basename
                    id_cmd = "install_name_tool -id " + new_find_id + " " + find_local_lib
                    # print("@rpath: " + id_cmd)
                    os.popen(id_cmd)
                else:
                    print("ERROR: Cannot find @rpath lib ", find_lib)
            change_cmd = "install_name_tool -change " + dep + " " + new_find_id + " " + lib
            # print("@rpath: " + change_cmd)
            os.popen(change_cmd)
            if (rpath_basename not in processed_libs):
                processed_libs.add(rpath_basename)
                relink(find_local_lib)
        elif dep.startswith('@loader_path'):
            loader_basename = ntpath.basename(dep)
            find_cmd = "find " + rpath_dir + " -name " + loader_basename
            # print("@loader_path: " + find_cmd)
            find = os.popen(find_cmd).readlines()
            if len(find) != 1:
                print("Error finding " + dep + ": Found " + repr(find))
                exit(-1);
                continue
            find_lib = find[0].rstrip(' \t\n')
            find_local_lib = local_lib_dir + "/" + loader_basename
            if not os.path.isfile(find_local_lib):
                if os.path.isfile(find_lib):
                    shutil.copyfile(find_lib, find_local_lib)
                    os.chmod(find_local_lib, 0755)
                else:
                    print("ERROR: Cannot find @rpath lib ", find_lib)
            if (loader_basename not in processed_libs):
                processed_libs.add(loader_basename)
                relink(find_local_lib)
        elif not dep.startswith('/usr/local/'):
            # print("Skipping system dependency: " + dep)
            continue
        else:
            dep_id = dep
            if os.path.isfile(dep):
                # print("otool -DX " + dep)
                dep_id = os.popen("otool -DX " + dep).readline().rstrip('\n')
            dep_basename = ntpath.basename(dep)
            new_dep_id = bundle_lib_path + "/" + dep_basename
            dep_local_lib = local_lib_dir + "/" + dep_basename
            find_cmd = "find " + rpath_dir + " -name " + dep_basename
            find_lib = dep
            find = os.popen(find_cmd).readlines()
            if len(find) > 1:
                print("Error finding " + dep + ": Found " + find)
                exit(-1);
                continue
            elif len(find) == 1:
                find_lib = find[0].rstrip(' \t\n')
            else:
                print("WARNING: Cannot find local copy of " + dep + " -- may cause SIGILL errors at runtime on older machines")
            if not os.path.isfile(dep_local_lib):
                if os.path.isfile(find_lib):
                    shutil.copyfile(find_lib, dep_local_lib)
                    os.chmod(dep_local_lib, 0755)
                    id_cmd = "install_name_tool -id " + new_dep_id + " " + dep_local_lib
                    # print(id_cmd)
                    os.popen(id_cmd)
                else:
                    print("ERROR: Cannot find source lib " + find_lib)
            change_cmd = "install_name_tool -change " + dep_id + " " + new_dep_id + " " + lib
            # print(change_cmd)
            os.popen(change_cmd)
            change_cmd = "install_name_tool -change " + dep + " " + new_dep_id + " " + lib
            # print(change_cmd)
            os.popen(change_cmd)
            if (dep_basename not in processed_libs):
                processed_libs.add(dep_basename)
                relink(dep_local_lib)


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