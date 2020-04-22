import base64

import cv2
import numpy as np
import requests


class AssetBoxImager(object):
    """Simple utility class that helps generate images that represent boxes found in an
    Asset's metadata.

    Args:
        asset(zmlp.Asset): Asset to generate box images for.
        zmlp_client(ZmlpClient): ZMLP client used to get information about the asset.

    """
    def __init__(self, asset, zmlp_client):
        self.asset = asset
        self.client = zmlp_client
        self._image = None

    @property
    def image(self):
        if self._image is not None:
            return self._image
        proxy = self.asset.get_thumbnail(3)
        response = self._download_image_from_zmlp(proxy.id)
        numpy_array = np.fromstring(response.content, np.uint8)
        self._image = cv2.imdecode(numpy_array, cv2.IMREAD_COLOR)
        return self._image

    def _download_image_from_zmlp(self, file_id):
        """Testing seam that downloads the bites of a file from ZMLP.

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


def crop_image_poly(image, poly, width=256, color=(255, 0, 0), thickness=3):
    """Function accepts an opencv image and returns a new image cropped to just show the
    poly given. The output image is also resized to the given width while maintaining
    the original aspect ratio. If the poly has more a less than 4 points then a polygon
    representing those points is drawn on the resulting image.

    Args:
        image (numpy.ndarray): Opencv image.
        poly (list): List of coordinates given as percentages. If len(poly)==4, it is assumed
         to be a bounding box. Otherwise it is a list of vertices. In this case the polygon is
         drawn and then the image cropped to the polygon's bounding box.
        color (List<int>): RGB values describing the color of the polygon.
        thickness (int): Thickness of the polygon drawn.

    Returns (numpy.ndarray): Opencv image cropped to show the box.

    """
    yr = image.shape[0]
    xr = image.shape[1]
    # If poly is four numbers, assume it's a bounding box. Otherwise it's a polygon
    if len(poly) == 4:
        x1 = int(poly[0] * xr)
        y1 = int(poly[1] * yr)
        x2 = int(poly[2] * xr)
        y2 = int(poly[3] * yr)
        draw = image
    else:
        point_list = []
        for i in range(0, len(poly), 2):
            x = int(poly[i] * xr)
            y = int(poly[i+1] * yr)
            point_list.append([x, y])
        pts = np.array([point_list], np.int32)
        pts = pts.reshape((-1, 1, 2))
        draw = cv2.polylines(image, [pts], True, color, thickness)
        # Now figure out where to crop, using the bounding box of all those points
        v_min = np.min(pts, axis=0)
        v_max = np.max(pts, axis=0)
        y1 = v_min[0][1] - thickness
        y2 = v_max[0][1]
        x1 = v_min[0][0] - thickness
        x2 = v_max[0][0]
    cropped_image = draw[y1:y2, x1:x2]
    xrc = cropped_image.shape[1]
    scale = width / xrc
    resized = cv2.resize(cropped_image, (0, 0), fx=scale, fy=scale)
    return resized
