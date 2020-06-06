#!/usr/bin/env python3
from setuptools import setup
from datetime import datetime

# See https://packaging.python.org/tutorials/packaging-projects/
# for details about packaging python projects

# Generating distribution archives (run from same directory as this file)
# python3 -m pip install --user --upgrade setuptools wheel
# python3 setup.py sdist bdist_wheel

requirements = [
    'zmlp',
    'zmlpsdk',
    'pandas',
    'opencv-python',
    'Pillow',
]

setup(
    name='zvi',
    version='1.0.1',
    description='Zorroa Visual Intelligence ML Environment',
    url='https://www.zorroa.com',
    license='Copyright ' + str(datetime.now().year) + ' Zorroa Corp. All Rights Reserved.',
    package_dir={'': 'pylib'},
    packages=['zvi'],
    scripts=[],
    classifiers=[
        'Programming Language :: Python :: 3',
        'License :: OSI Approved :: MIT License',
        'Operating System :: OS Independent'
    ],

    include_package_data=True,
    install_requires=requirements
)
