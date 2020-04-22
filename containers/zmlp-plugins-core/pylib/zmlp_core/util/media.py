import json
import logging
import os
import re
from subprocess import check_output, check_call, CalledProcessError, DEVNULL

import xmltodict
from pathlib import Path

from zmlpsdk.base import FileTypes
from zmlpsdk import file_storage

logger = logging.getLogger(__name__)


def media_size(path):
    """
    Return the width, height of the given media path.

    Args:
        path (string): Path the image of video media.

    Returns:
        (width, height): The media dimensions in pixels.
    """
    if Path(path).suffix[1:] in FileTypes.videos:
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
        cmd = ["oiiotool", "-q", "--wildcardoff", "--info", str(path)]
        try:
            logger.info("running command: %s" % cmd)
            line = [e for e in
                    check_output(cmd, shell=False, stderr=DEVNULL).decode().split(" ") if e]
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
           '-q',
           '--wildcardoff',
           '--info:format=xml:verbose=1',
           str(file_path)]

    logger.info("running command: %s" % cmd)

    # Have to remove bad unicode chars with decode
    output = check_output(cmd, shell=False, stderr=DEVNULL)
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


def ffprobe(src_path):
    """Returns the json results of an ffprobe command as a dictionary.

    Args:
        src_path (str): Path the the medis.

    Returns:
        dict: The media properties extracgted by ffprobe
    """
    cmd = ['ffprobe',
           '-v',
           'quiet',
           '-print_format',
           'json',
           '-show_streams',
           '-show_format',
           str(src_path)]

    logger.info("running command: %s" % cmd)
    ffprobe_result = check_output(cmd, shell=False)
    return json.loads(ffprobe_result)


def get_video_metadata(src_path):
    """
    Use FFProbe to extract video metadata.

    Args:
        src_path (str): The path to a video to FFPprobe

    Returns:
        (dict):
    """
    props = ffprobe(src_path)
    result = {}
    frame_rate = None
    frames = None

    for stream in props.get('streams', []):
        if stream.get('codec_type') == 'video':
            # Determine the frame rate.
            frame_rate = stream.get('r_frame_rate')
            if frame_rate:
                numerator, denominator = frame_rate.split('/')
                frame_rate = round(float(numerator) / float(denominator), 2)
            result['frameRate'] = frame_rate
            result['frames'] = stream.get('nb_frames')

            # Set the dimensions
            result['width'] = stream.get('width')
            result['height'] = stream.get('height')

    # Set the video duration.
    duration = props.get('format', {}).get('duration')
    if duration:
        duration = float(duration)
        result['length'] = duration
        if not frames:
            result['frames'] = int(duration * frame_rate)
    elif frame_rate and frames and not duration:
        result['length'] = float(frames) / float(frame_rate)

    # Set the title and description.
    result['description'] = props.get('format', {}).get('tags', {}).get('description')
    result['title'] = props.get('format', {}).get('tags', {}).get('title')
    result['timeCreated'] = props.get('format', {}).get('tags', {}).get('creation_time')
    return result


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
           "-v",
           "1",
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
        check_call(cmd, shell=False, stderr=DEVNULL)
    except CalledProcessError:
        # Don't let CalledProcessError bubble out
        # we're only sending IOError
        pass

    if not os.path.exists(destination_path):
        # Don't let the CalledProcessError impl detail leak out
        raise IOError('FFMpeg failed to create a thumbnail, command failed: {}'.format(cmd))


def set_resolution_attrs(asset, width, height):
    """Adds resolution metadata to the "media" namespace.

    Entries include height, width, aspect, and orientation.

    Args:
        width (number): Width of the asset in pixels.
        height (number): Height of the asset in pixels.

    """
    if width <= 0 or height <= 0:
        raise ValueError('Width and height must be greater than 0 to set '
                         'resolution metadata. %sx%s is invalid.' %
                         (width, height))
    asset.set_attr('media.width', int(width))
    asset.set_attr('media.height', int(height))
    aspect = round(float(width) / float(height), 2)
    asset.set_attr('media.aspect', aspect)
    if aspect <= 0.95:
        orientation = 'portrait'
    elif aspect <= 1.05:
        orientation = 'square'
    else:
        orientation = 'landscape'
    asset.set_attr('media.orientation', orientation)


def store_asset_proxy(asset, path, size, type="image", attrs=None):
    """
    A convenience function that adds a proxy file to the Asset and
    uploads the file to ZMLP storage.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        size (tuple of int): a tuple of width, height
        type (str): The media type
        attrs (dict): Additional media attrs
    Returns:
        dict: a ZMLP file storage dict.
    """
    _, ext = os.path.splitext(path)
    if not ext:
        raise ValueError('The path to the proxy file has no extension, but one is required.')
    name = '{}_{}x{}{}'.format(type, size[0], size[1], ext)
    proxy_attrs = asset.get_attr('tmp.{}_proxy_source_attrs'.format(type)) or {}
    proxy_attrs['width'] = size[0]
    proxy_attrs['height'] = size[1]
    if attrs:
        proxy_attrs.update(attrs)

    return file_storage.assets.store_file(path, asset, 'proxy', rename=name, attrs=proxy_attrs)
