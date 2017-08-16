#!/bin/bash
#
# [david] This pulls & builds zorroa-plugin-sdk and zorroa-server
# Then runs an analyst and an archivist locally in tmux tabs.
#
# Use the '-r' flag to re-run the servers without building plugins & common.
#
# Since I only run a local server occasionally, I usually need to rebuild
# everything, and I keep having to read the instructions and build manually
# every time, so this script automates a rebuild and run of the servers.
#
# Run this only after having gone through the install steps once & cloned the git repos.

if [[ "$1" != "-r" ]]; then
  git pull

  cd ../zorroa-plugin-sdk
  git pull
  mvn clean install -DskipTests

  cd ../zorroa-server/common
  mvn clean install -DskipTests
  cd ..

  cd ../zorroa-server
  mvn clean install -DskipTests
fi

tmux new-session -d -s zorroa -n archivist

tmux send-keys -t zorroa 'zos' C-m # this is a david-specific alias TODO add an env file
tmux send-keys -t zorroa 'cd archivist' C-m
tmux send-keys -t zorroa './run.sh' C-m

tmux new-window -t zorroa -n analyst

tmux send-keys -t zorroa 'zos' C-m # this is a david-specific alias TODO add an env file
tmux send-keys -t zorroa 'cd analyst' C-m
tmux send-keys -t zorroa './run.sh' C-m

tmux -2 attach -t zorroa
