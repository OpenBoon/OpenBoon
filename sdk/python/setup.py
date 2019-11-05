#!/usr/bin/env python
from setuptools import setup
from datetime import datetime

# See https://packaging.python.org/tutorials/packaging-projects/
# for details aout packaging python projects

# External pip dependencies are loaded from the requirements-zsdk.txt file. If any
# additional pip installs are required add them to that file.
with open('requirements.txt') as f:
    reqs = f.read().strip().splitlines()

setup(
    name='zorroa',
    version=open("VERSION").read().strip(),
    description='Zorroa Python SDK',
    url='https://www.zorroa.com',
    license='Copyright ' + str(datetime.now().year) + ' Zorroa Corp. All Rights Reserved.',
    package_dir={'': 'pylib'},
    packages=['zorroa',
              'zorroa.zclient',
              'zorroa.zsdk',
              'zorroa.zsdk.document',
              'zorroa.zsdk.util',
              'zorroa.zsdk.ofs',
              'zorroa.zsdk.zpsd',
              'zorroa.zsdk.zpsdebug',
              'zorroa.zsdk.zps'],
    scripts=['bin/zpsd', 'bin/zpsdebug'],

    classifiers=[
        "Programming Language :: Python :: 3",
        # "License :: OSI Approved :: MIT License",  # TODO Add license type
        "Operating System :: OS Independent",        # TODO confirm this
    ],

    include_package_data=True,
    install_requires=reqs
)
