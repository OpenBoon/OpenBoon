# Model Utilities
import logging
import os
import subprocess
import tempfile
import zipfile

import zmlp
from zmlpsdk import file_storage

logger = logging.getLogger(__name__)


def download_dataset(ds_id, style, dst_dir, ratio):
    """
    Download the dataset locally.  Shells out to an external tool
    which handles the downloads in parallel.

    Args:
        ds_id (str): The ID of the dataset.
        style (str): The format the DS should be written into.
        dst_dir (str): The directory to write the DataSet into.
        ratio: (int): The test/train ratio.
    """
    cmd = ['dataset-dl.py',
           ds_id,
           style,
           dst_dir,
           '--train-test-ratio',
           str(ratio)]

    logger.info('Running Cmd: {}'.format(cmd))
    subprocess.call(cmd, shell=False)


def publish_pipeline_module(name,
                            descr,
                            category,
                            type,
                            proc_name,
                            args):
    """
    Publish a Pipeline Module using a given processor and argument list.  If the module
    already exists it is left as is.

    Args:
        name (str): A partial name for the module, will be prefixed with "custom"
        descr (str): A description of the module.
        category (str): A category which is high level tech (Tensorflow, Google Vision, etc)
        type (str): The type of task the model will handle. (LabelDetection, TextDetection)
        proc_name (str): The full processor class name.
        args (dict): Arguments for the processor.

    """
    name = "custom-{}".format(name)
    body = {
        "name": name,
        "description": descr,
        "provider": "Zorroa",
        "category": category,
        "type": type,
        "supportedMedia": ["Images", "Documents"],
        "ops": [
            {
                "type": "APPEND",
                "apply": [
                    {
                        "className": proc_name,
                        "image": "zmlp/plugins-train",
                        "args": args
                    }
                ]
            }
        ]
    }

    app = zmlp.app_from_env()
    try:
        app.client.post("/api/v1/pipeline-modules", body)
    except zmlp.client.ZmlpDuplicateException:
        logger.info("The pipeline module {} already exists".format(name))


def upload_model_directory(src_dir, file_id):
    """
    Upload a directory containing model files to cloud storage.  The model
    file will be associated with the datasource it was created with.

    Args:
        src_dir (str): The source directory.
        file_id (str): A project file ID, this is provided in the processor args.

    Returns:
        StoredFile A stored file instance.
    """

    zip_file_path = zip_directory(src_dir, tempfile.mkstemp(prefix="model_", suffix=".zip")[1])
    print(zip_file_path)
    return file_storage.projects.store_file_by_id(zip_file_path, file_id, precache=False)


def zip_directory(src_dir, dst_file, zip_root_name=None):
    """
    Zip the given directory a file.

    Args:
        src_dir (str): The source directory.
        dst_file (str): The destination file.s

    Returns:
        str: The dst file.

    """

    def zipdir(path, ziph, root_name):
        for root, dirs, files in os.walk(path):
            for file in files:
                ziph.write(os.path.join(root, file),
                           os.path.join(root_name, root.replace(path, ""), file))

    src_dir = os.path.abspath(src_dir)
    zip_root_name = zip_root_name or os.path.basename(src_dir)
    zipf = zipfile.ZipFile(dst_file, 'w', zipfile.ZIP_DEFLATED)
    zipdir(src_dir + "/", zipf, zip_root_name)
    zipf.close()
    return dst_file
