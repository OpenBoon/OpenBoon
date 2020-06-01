import logging
import subprocess

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
