#!/bin/bash

# This script simply clones the zorroa-test-data repository do the
# directory one above the current.

set -ex

pushd ..

git clone git@github.com:Zorroa/zorroa-test-data.git --quiet

popd
