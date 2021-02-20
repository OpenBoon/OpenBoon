import base64
import cv2
import numpy as np
import requests

from django.urls import reverse


class AssetBoxImager(object):
    """Simple utility class that helps generate images that represent boxes found in an
    Asset's metadata.

    Args:
        asset(zmlp.Asset): Asset to generate box images for.
        zmlp_client(BoonClient): ZMLP client used to get information about the asset.

    """
    def __init__(self, asset, zmlp_client):
        self.asset = asset
        self.client = zmlp_client
        self._image = None

    @property
    def image(self):
        """Lazily downloads a proxy image of the Asset. The image is returned as a numpy
        array readable by open-cv as an image.

        """
        if self._image is not None:
            return self._image
        proxy = self.asset.get_thumbnail(3)
        response = self._download_image_from_zmlp(proxy.id)
        numpy_array = np.fromstring(response.content, np.uint8)
        self._image = cv2.imdecode(numpy_array, cv2.IMREAD_COLOR)
        return self._image

    def _download_image_from_zmlp(self, file_id):
        """Testing seam that downloads the bites of a file from boonsdk.

        Args:
            file_id(str): UUID of a file associated with the Asset that will be downloaded.

        Returns(Response): Response object that has the contents of the file.

        """
        path = f'/api/v3/files/_stream/{file_id}'
        return requests.get(self.client.get_url(path), verify=False,
                            headers=self.client.headers(), stream=True)

    def _add_box_images(self, item, width):
        """Recursively digs through a dictionary and adds images to any dictionary
        that has a "bbox" key. Note that the changes are done in place and this function
        does not return anything.

        Args:
            item(object): The object that is being recursed over.
            width(int): Width of the image that is generated for any boxes that are found.

        """
        if isinstance(item, list):
            for item in item:
                self._add_box_images(item, width)
        elif isinstance(item, dict):
            for key in list(item):
                value = item[key]
                if key == 'bbox':
                    cropped_image = crop_image_poly(self.image, value, width=width)
                    retval, buffer = cv2.imencode('.png', cropped_image)
                    image_str = base64.b64encode(buffer).decode("utf-8")
                    b64_image = f'data:image/png;base64, {image_str}'
                    item['b64_image'] = b64_image
                else:
                    self._add_box_images(value, width)
        else:
            return

    def get_attr_with_box_images(self, dot_path, width=255):
        """This function works similarly to the Asset.get_attr function except it adds a
        "b64_image" key to any dictionary that has a "bbox" key. The value of "b64_image"
        is a base64 encoded image str suitable for display in html.

        Args:
            dot_path(str):Dot-notation path of the attribute to return.
            width(int): Width of the images that are generated.

        Returns(dict): Value of the attribute with the additional image info.

        """
        field_value = self.asset.get_attr(dot_path)
        self._add_box_images(field_value, width)
        return field_value


def crop_image_poly(image, poly, width=256, draw=False, color=(255, 0, 0), thickness=3):
    """Function accepts an opencv image and returns a new image cropped to just show the
    poly given. The output image is also resized to the given width while maintaining
    the original aspect ratio. If the poly has more a less than 4 points then a polygon
    representing those points is drawn on the resulting image.

    Args:
        image (numpy.ndarray): Opencv image.
        poly (list): List of numbers given as percentages of image size. If len(poly)==4, it is
         assumed to be a bounding box. Otherwise it is a list of vertices. In this case the
         polygon is drawn and then the image cropped to the polygon's bounding box.
        width: (int): Width of the resulting cropped image.
        draw: (boolean): Whether to draw the outline of a list-or-vertices polygon.
        color (List<int>): RGB values describing the color of the polygon.
        thickness (int): Thickness of the polygon drawn.

    Returns (numpy.ndarray): Opencv image cropped to show the box.

    """
    yr = image.shape[0]
    xr = image.shape[1]

    # Ensure poly points are within (0,1)
    poly = [min(max(i, 0), 1) for i in poly]

    # If poly is four numbers, assume it's a bounding box. Otherwise it's a polygon
    if len(poly) == 4:
        x1 = int(poly[0] * xr)
        y1 = int(poly[1] * yr)
        x2 = int(poly[2] * xr)
        y2 = int(poly[3] * yr)
        image_draw = image
    else:
        point_list = []
        for i in range(0, len(poly), 2):
            x = int(poly[i] * xr)
            y = int(poly[i+1] * yr)
            point_list.append([x, y])
        pts = np.array([point_list], np.int32)
        pts = pts.reshape((-1, 1, 2))
        if draw:
            image_draw = cv2.polylines(image, [pts], True, color, thickness)
        else:
            image_draw = image
            # If we are not drawing, we reset the thickness so the crop is exact
            thickness = 0
        # Now figure out where to crop, using the bounding box of all those points
        v_min = np.min(pts, axis=0)
        v_max = np.max(pts, axis=0)
        # The crop rectangle is capped to the actual image boundaries
        y1 = max(0, v_min[0][1] - thickness)
        y2 = min(yr-1, v_max[0][1] + thickness)
        x1 = max(0, v_min[0][0] - thickness)
        x2 = min(xr-1, v_max[0][0] + thickness)
    cropped_image = image_draw[y1:y2, x1:x2]
    xrc = cropped_image.shape[1]
    if xrc > 0:
        scale = width / xrc
        resized = cv2.resize(cropped_image, (0, 0), fx=scale, fy=scale)
    else:
        resized = np.zeros((width, width, 3), np.uint8)

    return resized


def get_asset_style(item):
    """Set the AssetStyle for the frontend. Images and Documents are 'image'.

    Args:
        item: The specific asset item being acted upon, as returned by ZMLP.

    Returns:
        str: Whether this asset's source is primarily image or video
    """
    try:
        mimetype_base = item['metadata']['source']['mimetype'].split('/')[0]
        if mimetype_base == 'video':
            return 'video'
        else:
            return 'image'
    except (KeyError, IndexError):
        return None


def get_video_length(item):
    """Sets the video length for an asset for the frontend to use.

    Args:
        item: The specific asset item being acted upon, as returned by ZMLP.

    Returns:
        str: The length of this assets video, if it exists.
    """
    try:
        if item['assetStyle'] == 'video':
            return item['metadata']['media']['length']
        else:
            return None
    except KeyError:
        return None


def get_thumbnail_and_video_urls(request, item):
    """Determines the thumbnail image and video url to use for the frontend.

    For the thumbnail url, if an appropriate web-proxy is not found it will use the
    fallback image address. For the video url, if a suitable proxy video is not found
    (it may not have been created yet or the asset is an image) it will return None.

    Args:
        request: DRF Request object
        item: The specific asset item being acted upon, as returned by ZMLP.

    Returns:
        (str, str): Thumbnail and Video url for this asset item.
    """
    project_id = request.parser_context['view'].kwargs['project_pk']
    asset_id = item['id']
    thumbnail_url = '/icons/fallback_3x.png'
    thumbnail_category = 'web-proxy'
    video_proxy_url = None
    video_proxy_category = 'proxy'
    video_proxy_mimetype = 'video/mp4'
    for _file in item['metadata']['files']:
        if _file['category'] == thumbnail_category:
            name = _file['name']
            # If a web-proxy is found, build the file serving url for it
            thumbnail_url = reverse('file_name-detail', kwargs={'project_pk': project_id,
                                                                'asset_pk': asset_id,
                                                                'category_pk': thumbnail_category,
                                                                'pk': name})
        if _file['category'] == video_proxy_category and _file['mimetype'] == video_proxy_mimetype:
            name = _file['name']
            video_proxy_url = reverse('file_name-detail',
                                      kwargs={'project_pk': project_id,
                                              'asset_pk': asset_id,
                                              'category_pk': video_proxy_category,
                                              'pk': name})
    # Regardless of the url being used, make it absolute
    thumb = request.build_absolute_uri(thumbnail_url)
    if video_proxy_url:
        video = request.build_absolute_uri(video_proxy_url)
    else:
        video = None

    return thumb, video


def get_best_fullscreen_file_data(item):
    """Helper that looks at an assets files and determines the best one to view fullscreen.

    If no valid file is found for display, returns None.

    Args:
        item: The specific asset item being acted upon, as returned by ZMLP.

    Return:
        dict: The file blob that represents the best file for fullscreen viewing. None if
            a file is not found.
    """
    asset_style = get_asset_style(item)
    best_file = get_largest_proxy(item, asset_style)
    return best_file


def get_largest_proxy(item, mimetype_prefix='image'):
    """Looks at an assets files, and return the largest proxy file with the specified mimetype.

    Proxy files that are of category 'web-proxy' will be prioritized first.

    Args:
        item: The specific asset item being acted upon, as return by ZMLP.
        mimetype_prefix: The mimetype of the proxy that should be looked for. Defaults to images.

    Return:
        dict: The data for the largest proxy of the given mimetype.
    """
    _files = item['metadata']['files']
    if not _files:
        # Bail if there are no files to look at
        return None

    # Filter for the given mimetype
    def filter_by_mimetype(_file):
        if _file['mimetype'].startswith(mimetype_prefix):
            return True
        return False
    matching_mimetypes = list(filter(filter_by_mimetype, _files))

    # Sort matching files, largest resolution first
    def resolution_resolver(_file):
        return int(_file['attrs'].get('width', 0)) * int(_file['attrs'].get('height', 0))
    sorted_files = sorted(matching_mimetypes, key=resolution_resolver, reverse=True)

    # Check for web proxies in the sorted files first
    def filter_for_web_proxies(_file):
        if _file['category'] == 'web-proxy':
            return True
        return False
    sorted_web_proxies = list(filter(filter_for_web_proxies, sorted_files))
    if sorted_web_proxies:
        return sorted_web_proxies[0]

    # Return the just the largest proxy if there are no web-proxies
    def filter_for_proxies(_file):
        if _file['category'] == 'proxy':
            return True
        return False
    sorted_proxies = list(filter(filter_for_proxies, sorted_files))
    if sorted_proxies:
        return sorted_proxies[0]
    else:
        # Managed to not find any web-proxies or proxies
        return None
