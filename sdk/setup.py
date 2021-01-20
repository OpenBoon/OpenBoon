#!/usr/bin/env python3
from setuptools import setup
from datetime import datetime

# See https://packaging.python.org/tutorials/packaging-projects/
# for details about packaging python projects

# Generating distribution archives (run from same directory as this file)
# python3 -m pip install --user --upgrade setuptools wheel
# python3 setup.py sdist bdist_wheel

requirements = [
    "zvi-client",
    "minio",
    "google-cloud-storage>=1.20.0",
    "backoff",
    "pytest",
    "opencv-python-headless",
    "boto3",
    "azure-storage-blob",
    "opencv-python-headless",
    "opencv-contrib-python-headless",
    "Pillow",
    "pdoc3",
    "requests",
    "xmltodict"
]

setup(
    name='zmlpsdk',
    version=open("VERSION").read().strip(),
    description='ZMLP Python SDK',
    url='https://www.zorroa.com',
    license='Copyright ' + str(datetime.now().year) + ' Zorroa Corp. All Rights Reserved.',
    package_dir={'': 'pylib'},
    packages=['zmlpsdk'],
    scripts=['bin/dataset-dl.py'],
    classifiers=[
        "Programming Language :: Python :: 3",
        # "License :: OSI Approved :: MIT License",  # TODO Add license type
        "Operating System :: OS Independent",        # TODO confirm this
    ],

    include_package_data=True,
    install_requires=requirements
)
