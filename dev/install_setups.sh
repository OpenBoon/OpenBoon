#!/bin/bash
# setup.py in each directory

cd "$cwd" || return
source zenv/bin/activate

cd ./containers/zmlp-plugins-base/ || return
pwd
pip3 install .

cd ../../client/python/ || return
pwd
pip3 install .

cd ../../sdk/ || return
pwd
pip3 install .