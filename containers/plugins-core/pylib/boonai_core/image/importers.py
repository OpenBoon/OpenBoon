import operator
from datetime import datetime
from functools import reduce

import dateutil.parser
from pathlib import Path

from boonsdk import FileImport
from boonflow import AssetProcessor, Argument, ExpandFrame, FileTypes
from boonflow.storage import file_storage
from ..util.media import get_image_metadata, set_resolution_attrs


class ImageImporter(AssetProcessor):

    date_fields = ['Exif.DateTimeOriginal', 'Exif.DateTimeDigitized',
                   'Exif.DateTime', 'IPTC.DateCreated', 'IPTC.TimeCreated', 'DateTime',
                   'File.FileModifiedDate', 'Date']

    file_types = FileTypes.images

    def __init__(self):
        super(ImageImporter, self).__init__()
        self.add_arg(Argument('extract_image_pages', 'bool', default=False))

    def process(self, frame):
        asset = frame.asset
        path = Path(file_storage.localize_file(asset))
        metadata = get_image_metadata(path)
        set_resolution_attrs(asset, int(metadata.get('full_width')),
                             int(metadata.get('full_height')))

        asset.set_attr("media.type", "image")
        self.set_location(asset, metadata)
        self.set_date(asset, metadata)

        subimages = int(metadata.get('subimages', 1))
        asset.set_attr('media.length', subimages)

        page = asset.get_attr('media.pageNumber')
        if page == 1 and self.arg_value('extract_image_pages') and metadata.get('subimages'):
            self.extract_pages(frame, metadata)

    def set_date(self, document, metadata):
        """Extracts the date from the metadata and sets it on the document.

        Args:
            document(Document): Document to add date created information.
            metadata(dict): Metadata to parse a date from.

        """
        for field in self.date_fields:
            try:
                date_str = reduce(operator.getitem, field.split('.'), metadata)
            except (ValueError, KeyError, TypeError):
                continue
            date_str = date_str.replace('"', '')
            try:
                _datetime = datetime.strptime(date_str, '%Y:%m:%d %H:%M:%S')
            except ValueError:
                try:
                    _datetime = dateutil.parser.parse(date_str)
                except ValueError:
                    continue
            document.set_attr('media.timeCreated', _datetime.isoformat())
            break

    def set_location(self, document, metadata):
        """Extracts the location metadata and sets it as the media.latitude and
        media.longitude on the document.

        Args:
            document(Document): Document to add GPS location info to.
            metadata(dict): Metadata to parse for GPS location info.

        """
        gps_data = metadata.get('GPS', {})
        required_keys = ['LatitudeRef', 'Latitude', 'LongitudeRef', 'Longitude']
        if all(key in gps_data for key in required_keys):
            latitude = self._get_degrees_from_coordinates(gps_data['Latitude'])
            longitude = self._get_degrees_from_coordinates(gps_data['Longitude'])
            if gps_data['LatitudeRef'].lower() == 's':
                latitude *= -1
            if gps_data['LongitudeRef'].lower() == 'w':
                longitude *= -1
            if latitude and longitude:
                document.set_attr('location.point.lat', latitude)
                document.set_attr('location.point.lon', longitude)

    def _get_degrees_from_coordinates(self, coordinates):
        """Takes a coordinates string in the form "degrees, minutes, seconds" and returns
        it's decimal degrees.

        Args:
            coordinates(str): Coordinates string parsed from metadata.

        Returns:
            float: Decimal degree representation of the given coordinates.

        """
        if not coordinates:
            return 0
        if not isinstance(coordinates, (list, tuple)):
            coordinates = coordinates.split(',')
        if not len(coordinates) == 3:
            return 0
        degrees = float(coordinates[0])
        minutes = float(coordinates[1])
        seconds = float(coordinates[2])
        return degrees + (minutes / 60) + (seconds / 3600)

    def extract_pages(self, frame, metadata):
        """Extract pages from multi-page images. Each page is added as a derived frame.

        Args:
            frame(Frame): Parent to add derived frames to.
            metadata(dict): Metadata describing the parent image.
        """
        subimages = int(metadata.get('subimages'))
        source_asset = "asset:{}".format(frame.asset.id)
        for i in range(2, subimages + 1):
            expand = ExpandFrame(FileImport(source_asset, page=i))
            self.expand(frame, expand)
