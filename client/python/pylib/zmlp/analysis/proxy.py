import os
import tempfile

import cv2

from zmlpsdk.storage import file_storage

__all__ = [
    'store_asset_proxy',
    'store_element_proxy',
    'get_proxy_level'
]


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
    return file_storage.assets.store_file(asset, path, 'proxy', rename=name, attrs=proxy_attrs)


def get_proxy_level(asset, level, mimetype="image/"):
    """
    Localize and return the given proxy level.  The smallest proxy is
    level 0, the largest proxy is 0 or greater.  Out of bounds level
    values will be clamped to the correct range automatically.  For example
    if there are only 2 proxies and you pass level 3, then you will get the
    level 2 proxy.

    Args:
        asset (Asset): The Asset.
        level (int): The proxy level, the larger the number the bigger the file.
        mimetype (str): The proxy mimetype, defaults to image/

    Returns:
        str: A path to the localized proxy file or None on no match.

    """
    files = asset.get_files(mimetype=mimetype, category='proxy', attr_keys=['width'],
                            sort_func=lambda f: f['attrs']['width'])
    if level >= len(files):
        level = -1
    try:
        proxy = files[level]
        return file_storage.assets.localize_file(asset, proxy)
    except IndexError:
        return None


def store_element_proxy(asset, img, name, rects=None, labels=None, color=None):
    """
    Store an element proxy to the Archivist.

    Note that, if you pass labels, you need to pass one label for evert rect.

    Args:
        asset (Asset): The asset
        img (cvImage): An openCV image
        name (str): An identifying name for the image.
        rects (list[list]): A list of rects to draw.
        labels: (list): A list of labels to draw.
        color (tuple): A BGR tuple for box or label colors. Color only matters if you have rects.

    Returns:
        dict: a file storage dictionary which can be provided to an Element instance.

    """
    if rects and labels:
        if len(rects) != len(labels):
            raise ValueError(
                "The number of rects and labels must be equal. {}!={}".format(
                    len(rects), len(labels)))

    if rects:
        if not color:
            color = (255, 0, 0)
        for i, rect in enumerate(rects):
            cv2.rectangle(img, (rect[0], rect[1]), (rect[2], rect[3]),
                          color, 2, cv2.LINE_AA)
            if labels:
                cv2.putText(img, ",".join(labels[i]), (rect[2], max(0, rect[3] - 10)),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.50, color, 1, cv2.LINE_AA)

    with tempfile.NamedTemporaryFile(suffix=".jpg") as tf:
        cv2.imwrite(tf.name, img)
        attrs = {"width": img.shape[1], "height": img.shape[0]}
        rename_to = "{}_{}x{}.jpg".format(name, attrs['width'], attrs['height'])

        return file_storage.assets.store_file(asset,
                                              tf.name,
                                              'element',
                                              rename=rename_to,
                                              attrs=attrs)
