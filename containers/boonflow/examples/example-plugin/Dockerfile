# Inherit from the ZVI plugin base image
FROM boonai/plugins-base:latest

# Copy your code to the /zvi/pylib directory
COPY my_plugin /zvi/pylib

# Optional: Install additional libraries or tools un Ubuntu repo.
RUN apt-get update && apt-get -y upgrade && \
    apt-get --no-install-recommends -y install \
    libmagic-dev \
     --no-install-recommends

# Optional: Copy a python requirements file and install additional
# python dependencies
COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt
RUN rm requirements.txt

