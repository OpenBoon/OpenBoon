#!/bin/bash

cwd=$(pwd)

echo "Installing virtualenv"
pip3 install --upgrade virtualenv

echo "Creating Virtual Environment..."
virtualenv -p python3.7 zenv

echo "Activating Virtual Environment"
source zenv/bin/activate

echo "Installing packages..."
chmod +x install_requirements.sh install_setups.sh

echo "Installing requirements.txt"
source install_requirements.sh
cd "$cwd" || return

echo "Installing setup.py"
source install_setups.sh

echo "Complete"
cd "$cwd" || return