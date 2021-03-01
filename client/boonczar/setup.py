#!/usr/bin/env python3
from setuptools import setup

# See https://packaging.python.org/tutorials/packaging-projects/
# for details about packaging python projects

# Generating distribution archives (run from same directory as this file)
# python3 -m pip install --user --upgrade setuptools wheel
# python3 setup.py sdist bdist_wheel

requirements = [
    "requests",
    "pyOpenSSL",
    "PyJWT>=2.0",
    "backoff",
    "pytest"
]

setup(
    name='boonczar',
    version="1.0.0",
    description='Boon AI internal admin library',
    url='http://www.boonai.io',
    license='Proprietary',
    package_dir={'': 'pylib'},
    packages=['boonczar', 'boonczar.app', 'boonczar.entity',
              'boonczar.tools', 'boonczar.tools.bz'],
    scripts=['bin/bz'],
    author="Boon Dev",
    author_email="support@boonai.io",
    keywords="machine learning artificial intelligence",
    python_requires='>=3.4',

    classifiers=[
        "Programming Language :: Python :: 3",
        "Operating System :: OS Independent"
    ],

    include_package_data=True,
    install_requires=requirements
)
