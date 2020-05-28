#!/usr/bin/env python3
from setuptools import setup
from datetime import datetime

# See https://packaging.python.org/tutorials/packaging-projects/
# for details about packaging python projects

# Generating distribution archives (run from same directory as this file)
# python3 -m pip install --user --upgrade setuptools wheel
# python3 setup.py sdist bdist_wheel

requirements = [
    "requests",
    "pyOpenSSL",
    "PyJWT",
    "backoff",
    "pytest",
]

setup(
    name='zmlp',
    version=open("VERSION").read().strip(),
    description='ZMLP Python Client',
    url='https://www.zorroa.com',
    license='Copyright ' + str(datetime.now().year) + ' Zorroa Corp. All Rights Reserved.',
    package_dir={'': 'pylib'},
    packages=['zmlp', 'zmlp.app', 'zmlp.entity'],
    scripts=[],

    classifiers=[
        "Programming Language :: Python :: 3",
        # "License :: OSI Approved :: MIT License",  # TODO Add license type
        "Operating System :: OS Independent",        # TODO confirm this
    ],

    include_package_data=True,
    install_requires=requirements
)
