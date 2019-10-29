#!/bin/sh

build_order=(
    "plugins-py3-base"
    "plugins-py3-media"
    "plugins-py3-core"
)

for d in "${build_order[@]}" ; do
    echo $d
    cd $d
    docker build . -t $d
    cd ..
done

