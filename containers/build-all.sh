#!/bin/sh

build_order=(
    "plugins-py3-base"
    "plugins-py3-core"
)

for d in "${build_order[@]}" ; do
    echo $d
    cd $d
    docker build . --no-cache -t $d
    cd ..
done

