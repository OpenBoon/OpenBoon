#!/usr/bin/env python3
from setuptools import setup

# See https://packaging.python.org/tutorials/packaging-projects/
# for details about packaging python projects

# Generating distribution archives (run from same directory as this file)
# python3 -m pip install --user --upgrade setuptools wheel
# python3 setup.py sdist bdist_wheel

requirements = [
    'cmake',
    'boonsdk',
    'pandas',
    'matplotlib',
    'opencv-python',
    'Pillow',
    'ipython',
    'opencv_python',
    'scikit-learn',
    'bokeh',
    'holoviews',
    'MulticoreTSNE'
]

setup(
    name='boonlab',
    version='1.0.0',
    description='Boon AI Jupyter Environment',
    url='https://www.boonai.io',
    license='Apache2',
    package_dir={'': 'pylib'},
    packages=['boonlab'],
    scripts=[],
    classifiers=[
        'Programming Language :: Python :: 3',
        'Operating System :: OS Independent'
    ],

    include_package_data=True,
    install_requires=requirements
)
