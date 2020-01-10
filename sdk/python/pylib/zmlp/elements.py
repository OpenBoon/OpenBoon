import collections

from zmlp.util import as_collection


class Element(object):
    """
    An Element describes a region within an image which contains a specific prediction,
    such as a face or object.  Elements are stored as nested objects in ElasticSearch
    which allows for specific combinations of types, labels, regions, etc to be searched.

    Elements are considered unique by type, labels, rect, and stored_file name.

    Attributes:
        regions (list[str]): The region of the image where the Element exists. This
            is automatically set if it can be calculated from the rect and
            the stored_file size.

    """
    def __init__(self, type, labels, score=None, rect=None, proxy_file=None, vector=None):
        """
        Create a new Element instance.

        If a rect and stored_file arg with width/height attributes is provided, the
        element regions will be calculated automatically.

        Args:
            type (str): The type of element, typically 'object' or 'face' but
                it can be an arbitrary value.
            labels (list[str]): A list of predicted labels.
            score (float): If a prediction is made, a score describes the confidence level.
            rect (list[int]): A list of 4 integers describe the rectangle containing the element.
                The ints represent the upper left point and lower left point of the rectangle.
            proxy_file (dict): A stored file record which contains a proxy image for the Element.
            vector (str): The similarity vector.
        """
        self.type = type
        self.labels = as_collection(labels)
        self.score = score
        self.rect = rect
        self.vector = vector

        if proxy_file:
            self.file = '{}/{}'.format(proxy_file['category'], proxy_file['name'])
        else:
            self.file = None

        if self.rect and proxy_file:
            self.regions = self.calculate_regions(proxy_file)
        else:
            self.regions = None

    def calculate_regions(self, stored_file):
        """
        Calculate the regions where the element exists.  Possible
        value are:
            - NW
            - NE
            - SW
            - SE
            - CENTER

        Args:
            stored_file (dict): A stored file dict.

        Returns:
            list[str]: An array of regions or None if no regions can be calculated.

        """
        Point = collections.namedtuple("Point", "x y")

        l1 = Point(self.rect[0], self.rect[1])
        r1 = Point(self.rect[2], self.rect[3])

        # Use rect to determine region
        keys = stored_file.get('attrs', {}).keys()
        if 'width' in keys and 'height' in keys:
            width = stored_file['attrs']['width']
            height = stored_file['attrs']['height']
            regions = {
                'NW': (Point(0, 0), Point(width / 2, height / 2)),
                'NE': (Point(width / 2, 0), Point(width, height / 2)),
                'SW': (Point(0, height / 2), Point(width / 2, height)),
                'SE': (Point(width / 2, height / 2), Point(width, height))
            }
            result = []
            for reg, points in regions.items():
                if l1.x > points[1].x or points[0].x > r1.x:
                    continue
                if l1.y > points[1].y or points[0].y > r1.y:
                    continue
                result.append(reg)
            # Add Center if we're in all 4
            if len(result) == 4:
                result.append("CENTER")
            return result or None
        return None

    def for_json(self):
        """
        Serialize the Element to JSON.
        Returns:
            dict: A serialized Element
        """
        serializable_dict = {}
        attrs = ['type', 'labels', 'rect', 'score', 'file', 'regions']
        for attr in attrs:
            if getattr(self, attr, None) is not None:
                serializable_dict[attr] = getattr(self, attr)
        return serializable_dict
