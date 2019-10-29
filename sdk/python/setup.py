#!/usr/bin/env python
from setuptools import setup

# External pip dependencies are loaded from the requirements-zsdk.txt file. If any
# additional pip installs are required add them to that file.
with open('requirements.txt') as f:
    reqs = f.read().strip().splitlines()

setup(
    name='zorroa',
    version=open("VERSION").read().strip(),
    description='Zorroa Python SDK',
    include_package_data=True,
    url='http://www.zorroa.com',
    download_url='https://dl.zorroa.com/public/wheels/zsdk-0.41.0-py2-none-any.whl',
    license='Copyright 2019 Zorroa Corp. All Rights Reserved.',
    package_dir={'': 'pylib'},
    packages=['zorroa',
              'zorroa.client',
              'zorroa.zsdk',
              'zorroa.zsdk.document',
              'zorroa.zsdk.util',
              'zorroa.zsdk.ofs',
              'zorroa.zsdk.zpsd',
              'zorroa.zsdk.zpsdebug',
              'zorroa.zsdk.zps'],
    scripts=['bin/zpsd', 'bin/zpsdebug'],
    install_requires=reqs
)
