from zmlpsdk.proxy import get_proxy_level_path
from zmlpsdk.base import ExpandFrame
from zmlp import Clip, FileImport


def check_video_clip_preconditions(asset):
    """
    Return true if the given asset can be clipified.

    Args:
        asset (Asset): The asset which might be clipified.

    Returns:
        bool: True if all preconditions are met

    """
    # Only video can be clipped
    if asset.get_attr('media.type') != 'video':
        return False

    # Only full videos can be clipped
    if asset.get_attr('clip.timeline') != 'full':
        return False

    if not get_proxy_level_path(asset, 3, "video/"):
        return False

    return True


def make_video_clip_file_import(asset, cut_in, cut_out, timeline):
    """
    Make an FileImport wrapped in an ExpandFrame that can be
    emitted back to the archivist using the parent asset and given clip
    args.

    Args:
        asset:
        cut_in:
        cut_out:
        timeline:

    Returns:

    """
    clip = Clip.scene(cut_in, cut_out, timeline)
    file_import = FileImport("asset:{}".format(asset.id), clip=clip)
    # Copy media onto the new asset since it's going to be
    # exactly the same.
    file_import.attrs["media"] = asset.get_attr('media')
    return ExpandFrame(file_import)
