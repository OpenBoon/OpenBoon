import json
import logging
import subprocess
import tempfile


logger = logging.getLogger(__name__)


def extract_audio_file(src_path):

    if not (has_audio_channel(src_path)):
        msg = f'The file {src_path} does not have an audio channel.'
        logger.warning(msg)
        raise RuntimeError(msg)

    audio_channels = 2
    audio_sample_rate = 44100

    audio_file = tempfile.mkstemp(suffix=".flac", prefix="audio", )[1]
    cmd_line = ['ffmpeg',
                '-v',
                'quiet',
                '-y',
                '-i', src_path,
                '-vn',
                '-acodec', 'flac',
                '-ar', str(audio_sample_rate),
                '-ac', str(audio_channels),
                audio_file]

    logger.info('Executing {}'.format(" ".join(cmd_line)))
    subprocess.check_call(cmd_line)

    return audio_file


def has_audio_channel(src_path):
    """Returns true if the video has at least 1 audio channel.

    Args:
        src_path (str): Path the the media.

    Returns:
        True is media has at least one audio stream, False otherwise.
    """

    cmd = ['ffprobe',
           str(src_path),
           '-show_streams',
           '-select_streams', 'a',
           '-print_format', 'json',
           '-loglevel', 'error']

    logger.debug("running command: %s" % cmd)
    ffprobe_result = subprocess.check_output(cmd, shell=False)
    n_streams = len(json.loads(ffprobe_result)['streams'])
    return n_streams > 0
