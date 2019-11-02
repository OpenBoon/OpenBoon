import json
import logging
import xmltodict
import re
import magic

from zorroa.zsdk.util.std import file_exists
from pathlib2 import Path
from subprocess import check_output, check_call, CalledProcessError

logger = logging.getLogger(__name__)


def media_size(path):
    """
    Return the width, height of the given media path.

    Args:
        path (string): Path the image of video media.

    Returns:
        (width, height): The media dimensions in pixels.
    """
    # Formats that may not be detected as video/* but need to use
    # ffprobe for metadata extraction.
    other_ffrobe_formats = [".mxf"]

    mimetype = magic.detect_from_filename(path).mime_type

    if mimetype.startswith("video/") or Path(path).suffix in other_ffrobe_formats:
        cmd = ["ffprobe",
               "-v",
               "error",
               "-select_streams",
               "v:0",
               "-show_entries",
               "stream=width,height",
               "-of",
               "csv=s=x:p=0",
               str(path)]

        try:
            logger.info("running command: %s" % cmd)
            size = check_output(cmd, shell=False).decode().split("x")
            return int(size[0]), int(size[1])
        except CalledProcessError:
            raise ValueError("Invalid video file, unable to determine size: '{}".format(path))

    else:
        # Oiiotool supports the most image formats.
        # On large files, PIL blows up with PIL.Image.DecompressionBombError:
        # Image size (264192000 pixels) exceeds limit of 178956970 pixels,
        # could be decompression bomb DOS attack.
        cmd = ["oiiotool", "--wildcardoff", "--info", str(path)]
        try:
            logger.info("running command: %s" % cmd)
            line = [e for e in check_output(cmd, shell=False).decode().split(" ") if e]
            idx = line.index("x")
            return int(line[idx-1]), int(re.sub('[^0-9]', '', line[idx+1]))
        except CalledProcessError:
            raise ValueError("Invalid image file, unable to determine size: '{}".format(path))


def get_image_metadata(file_path):
    """Extract and return image metadata from the given file path. Path must point
    to an image.

    Args:
        file_path: (str):

    Returns:
        :obj:`dict`: The image metadata.

    """
    cmd = ['oiiotool',
           '--wildcardoff',
           '--info:format=xml:verbose=1',
           str(file_path)]

    logger.info("running command: %s" % cmd)

    # Have to remove bad unicode chars with decode
    output = check_output(cmd, shell=False)
    if isinstance(output, (bytes, bytearray)):
        output = output.decode('ascii', errors='ignore')
    output = re.sub(r"&#\d+;", "", output)

    metadata = xmltodict.parse(output).get('ImageSpec')

    attribs = metadata.get('attrib')
    if attribs:
        del metadata['attrib']
        for attrib in attribs:
            key = attrib['@name']
            if '@description' in attrib:
                value = attrib['@description']
            else:
                value = attrib.get('#text')
                _type = attrib.get('@type')
                if not value or not _type:
                    continue
                if _type == 'uint':
                    value = int(value)
                elif _type == 'float':
                    value = float(value)
            if ':' in key:
                key, subkey = key.split(':', 1)
                if key not in metadata:
                    metadata[key] = {}
                metadata[key][subkey] = value
            else:
                metadata[key] = value
    return metadata


def get_output_dimension(size, width, height):
    """Returns a correctly scaled width and height based on a pixel length.

    Args:
        size(int): Pixel size to use for the longest side of the resolution.
        width(int): The width of the image to determine scaled dimensions for.
        height(int): The height of the image to determine scaled dimensions
            for.

    Returns:
        int, int: Returns a tuple of the calculated width and height.

    """
    size = float(size)
    source_height = float(height)
    source_width = float(width)
    if source_height > source_width:
        width = size / (source_height / source_width)
        height = size
    else:
        width = size
        height = size / (source_width / source_height)
    return int(width), int(height)


def ffprobe(destination_path):
    """Returns the json results of an ffprobe command as a dictionary.

    Args:
        destination_path (str): temporary location to store ffprobe results.

    Returns:
        :obj:`dict`: The ffprobe result
    """
    cmd = ['ffprobe',
           '-v',
           'quiet',
           '-print_format',
           'json',
           '-show_streams',
           '-show_format',
           str(destination_path)]

    logger.info("running command: %s" % cmd)
    ffprobe_result = check_output(cmd, shell=False)
    return json.loads(ffprobe_result)


def create_video_thumbnail(source_path, destination_path, seconds):
    """Creates a thumbnail image from the video at the specified seconds.

    Args:
        source_path (str or Path): Path to the source video.
        destination_path (str or Path): Path where the thumbnail should be
            created.
        seconds (float): The time in the video where the thumbnail should be
            taken from.

    Raises:
        (IOError): If the thumbnail could not be created.

    """
    cmd = ["ffmpeg",
           "-y",
           "-ss",
           str(seconds),
           "-i",
           str(source_path),
           "-vframes",
           "1",
           str(destination_path)]

    logger.info("running command: %s" % cmd)
    try:
        check_call(cmd, shell=False)
    except CalledProcessError as e:
        # Don't let CalledProcessError bubble out
        # we're only sending IOError
        pass

    if not file_exists(destination_path):
        # Don't let the CalledProcessError impl detail leak out
        raise IOError('FFMpeg failed to create a thumbnail, command failed: {}'.format(cmd))
