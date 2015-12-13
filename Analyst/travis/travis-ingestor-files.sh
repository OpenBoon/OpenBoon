#!/bin/bash
set -ex

wget http://zorroa.com/travis/models-caffe.tgz
tar xvzf models-caffe.tgz

wget http://zorroa.com/travis/lib.tgz
mv lib.tgz /var/tmp
pushd /usr/local
tar kxvzf /var/tmp/lib.tgz || true
popd

ls -al /usr/local/lib

