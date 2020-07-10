import logging
import subprocess

from zmlp.util import as_id

logger = logging.getLogger(__name__)


def download_labeled_images(model, style, dst_dir, ratio):
    """
    Download the dataset locally.  Shells out to an external tool
    which handles the downloads in parallel.

    Args:
        model (Model): A ZMLP Model or a unique Model ID.
        style (str): The on disk format the data should be written into.
        dst_dir (str): The destination directory.
        ratio: (int): The test/validation ratio.
    """
    cmd = ['dataset-dl.py',
           as_id(model),
           style,
           dst_dir,
           '--training-set-split',
           str(ratio)]

    logger.info('Running Cmd: {}'.format(cmd))
    subprocess.call(cmd, shell=False)
