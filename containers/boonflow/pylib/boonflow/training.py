import logging
import subprocess

from boonsdk.util import as_id

logger = logging.getLogger(__name__)


def download_labeled_images(model, style, dst_dir, ratio=0.0):
    """
    Download the dataset locally.  Shells out to an external tool
    which handles the downloads in parallel.

    Args:
        model (Model): A Boon AI Model or a unique Model ID.
        style (str): The on disk format the data should be written into.
        dst_dir (str): The destination directory.
        ratio: (float): The train/validation ratio, defaults to 0.
    """
    cmd = ['dataset-dl.py',
           as_id(model),
           style,
           dst_dir,
           '--validation-split',
           str(ratio)]

    logger.info('Running Cmd: {}'.format(cmd))
    subprocess.call(cmd, shell=False)
