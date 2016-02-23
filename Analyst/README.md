# Analyst

Archivist plugins and analysis execution servlet.

Multiple Analyst nodes can be run to perform cluster-based ingestion
coordinated by the Archivist. Start the Archivist before starting any
Analyst nodes. A simple way to start a single Analyst node on the local
machine is using the `run.sh` script. Running `mvn clean` will delete
the database and proxy files created by the local Analyst.


## Model Files

Many ingestors require large model files to perform detection or other operations.
Model files are stored in the model subdirectory and distributed with the platform.
At runtime, models are accessed through the `ZORROA_MODEL_PATH` environment variable.
When running the Analyst, make sure to set this environment variable so that the ingestors
can access their model files.


## Native Libraries

We use the JavaCPP framework to wrap and distribute native code libraries used
by individual ingestors, such as OpenCV or Caffe.
 
Information on the available libraries, as well as Javadoc for each library can
be found on the JavaCPP Presets page:

https://github.com/bytedeco/javacpp-presets


