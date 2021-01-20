import json
import logging
import os
import re
from pathlib import Path
from subprocess import check_output, CalledProcessError, DEVNULL

import xmltodict
from zmlpsdk import file_storage
from zmlpsdk.base import FileTypes

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
            logger.debug("running command: %s" % cmd)
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
            logger.debug("running command: %s" % cmd)
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

    logger.debug("running command: %s" % cmd)

    # Have to remove bad unicode chars with decode
    output = check_output(cmd, shell=False, stderr=DEVNULL)
    if isinstance(output, (bytes, bytearray)):
        output = output.decode('ascii', errors='ignore')
    output = re.sub(r"&#\d+;", "", output)

    metadata = xmltodict.parse(output).get('ImageSpec')
    ext_attribs = metadata.get('attrib')

    if ext_attribs:
        # We're going to flatten 'ext_attribs' list of ordered dict.
        # into the main dict.  So we can delete it from here.
        del metadata['attrib']

        def as_list(val):
            if isinstance(val, (list, set, tuple)):
                return val
            else:
                return [val]

        # This handles all the EXIF tags though the vast
        # majority are not used.
        for attrib in as_list(ext_attribs):
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

    logger.debug("running command: %s" % cmd)
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
            result['videoCodec'] = stream.get('codec_name')

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


def store_media_proxy(asset, path, proxy_type, size=None, attrs=None):
    """
    A convenience function that adds a media proxy file to the Asset and
    uploads the file to ZMLP storage.  Media proxies always have
    a height and a width.

    Args:
        asset (Asset): The purpose of the file, ex proxy.
        path (str): The local path to the file.
        proxy_type (str): The type of proxy, image or video.
        size (tuple of int): a tuple of width, height, None will auto-detect size
        attrs (dict): Additional media attrs
    Returns:
        dict: a ZMLP file storage dict.
    """
    _, ext = os.path.splitext(path)
    if not ext:
        raise ValueError('The path to the proxy file has no extension, but one is required.')

    # Combine all the attts
    final_attrs = {}

    proxy_attrs = asset.get_attr('tmp.{}_proxy_source_attrs'.format(proxy_type))
    if proxy_attrs:
        final_attrs.update(proxy_attrs)

    if attrs:
        final_attrs.update(attrs)

    # If the proxy is a video type, get some video details.
    if proxy_type == 'video':
        props = get_video_metadata(path)
        size = (props['width'], props['height'])
        final_attrs.update({
            'frames': props['frames'],
            'frameRate': props['frameRate'],
            'width': props['width'],
            'height': props['height']})

    if not size:
        size = media_size(path)

    if 'width' not in final_attrs:
        final_attrs['width'] = size[0]
        final_attrs['height'] = size[1]

    name = '{}_{}x{}{}'.format(proxy_type, size[0], size[1], ext)
    return file_storage.assets.store_file(path, asset, 'proxy', rename=name, attrs=final_attrs)


class MediaInfo:
    """
    Simple wrapper around the mediainfo tool.  Eventually this tool will be
    used for detecting image properties.
    """

    def __init__(self, path):
        cmd = [
            'mediainfo',
            '-f',
            '--Output=JSON',
            path
        ]
        self.attrs = json.loads(check_output(cmd, shell=False))

    def is_streamable(self):
        """
        Return true of the video file's moov atom is at the front of the
        fike, aka +faststart.

        Returns:
            bool: true if the video file is streamable.
        """
        entry = self.track('general')
        return entry.get('IsStreamable', 'No') == 'Yes'

    def track(self, ttype):
        """
        Get info for the given track. The possible types
        are general, video, and audio.

        Args:
            ttype (str): The track type.

        Returns:
            dict: An arbitrary dictionary of data.
        """
        ttype = ttype.lower()
        for track in self.attrs['media']['track']:
            if track.get("@type").lower() == ttype:
                return track
        return None
