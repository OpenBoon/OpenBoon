#!/bin/bash

cd "$cwd" || return
source zenv/bin/activate

pip3 install -r ./sdk/examples/example-plugin/requirements.txt
pip3 install -r ./applications/workbench/requirements.txt
pip3 install -r ./applications/sandbox/requirements.txt
pip3 install -r ./containers/zmlp-plugins-analysis/requirements.txt
pip3 install -r ./containers/zmlp-plugins-base/requirements.txt
pip3 install -r ./containers/zmlp-py3-base/requirements.txt
pip3 install -r ./containers/zmlp-plugins-train/requirements.txt
pip3 install -r ./containers/zmlp-plugins-models/requirements.txt
pip3 install -r ./containers/zmlp-plugins-core/requirements.txt
pip3 install -r ./services/analyst/requirements.txt
pip3 install -r ./services/mlbbq/requirements.txt