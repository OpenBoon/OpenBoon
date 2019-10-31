#!/bin/sh

cd ../sdk/python
rm -rf build
rm -rf dist
python3 setup.py bdist_wheel
cp dist/*.whl ../../containers/plugins-py3-base

cd ../../containers

build_order=(
    "plugins-py3-base"
    "plugins-py3-core"
    "plugins-py3-analysis"
)

for d in "${build_order[@]}" ; do
    echo $d
    cd $d
    docker build . --no-cache -t $d
    cd ..
done

