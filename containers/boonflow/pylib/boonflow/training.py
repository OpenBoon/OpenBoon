import logging
import subprocess

from boonsdk.util import as_id

logger = logging.getLogger(__name__)


def download_labeled_images(ds, style, dst_dir, ratio=0.0):
    """
    Download the dataset locally.  Shells out to an external tool
    which handles the downloads in parallel.

    Args:
        ds (DataSet): A Boon AI DataSet, a unique DataSet ID, or Model instance
        style (str): The on disk format the data should be written into.
        dst_dir (str): The destination directory.
        ratio: (float): The train/validation ratio, defaults to 0.
    """
    # Handles the case if they pass in a model or something with
    # a dataset_id.
    dataset_id = getattr(ds, 'dataset_id', as_id(ds))

    cmd = ['dataset-dl.py',
           dataset_id,
           style,
           dst_dir,
           '--validation-split',
           str(ratio)]

    logger.info('Running Cmd: {}'.format(cmd))
    subprocess.call(cmd, shell=False)
