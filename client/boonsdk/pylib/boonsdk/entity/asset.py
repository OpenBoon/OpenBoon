import csv
import json
import logging
import os

from .dataset import LabelScope
from .storage import StoredFile
from ..client import to_json
from ..util import as_collection

__all__ = [
    'Asset',
    'FileImport',
    'FileUpload',
    'FileTypes',
    'CsvFileImport'
]

logger = logging.getLogger(__name__)


class DocumentMixin(object):
    """
    A Mixin class which provides easy access to a deeply nested dictionary.
    """

    def __init__(self):
        self.document = {}

    def set_attr(self, attr, value):
        """Set the value of an attribute.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            value (:obj:`object`): value: The value for the particular
                attribute. Can be any json serializable type.
        """
        self.__set_attr(attr, value)

    def del_attr(self, attr):
        """
        Delete the attribute from the document.  If the attribute does not exist
        or is protected by a manual field edit then return false.  Otherwise,
        delete the attribute and return true.

        Args:
            attr (str): The attribute name.

        Returns:
            bool: True if the attribute was deleted.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts[0:-1]:
            if not isinstance(doc, dict) or k not in doc:
                return False
            doc = doc.get(k)

        attr_name = parts[-1]
        try:
            del doc[attr_name]
            return not self.attr_exists(attr)
        except KeyError:
            return False

    def get_attr(self, attr, default=None):
        """Get the given attribute to the specified value.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            default (:obj:`mixed`) The default value if no attr exists.

        Returns:
            mixed: The value of the attribute.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts:
            if not isinstance(doc, dict) or k not in doc:
                return default
            doc = doc.get(k)
        return doc

    def attr_exists(self, attr):
        """
        Return true if the given attribute exists.

        Args:
            attr (str): The name of the attribute to check.

        Returns:
            bool: true if the attr exists.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts[0:len(parts) - 1]:
            if k not in doc:
                return False
            doc = doc.get(k)
        return parts[-1] in doc

    def add_analysis(self, name, val):
        """Add an analysis structure to the document.

        Args:
            name (str): The name of the analysis
            val (mixed): the value/result of the analysis.

        """
        if not name:
            raise ValueError("Analysis requires a unique name")
        attr = "analysis.%s" % name
        if val is None:
            self.set_attr(attr, None)
        else:
            self.set_attr(attr, json.loads(to_json(val)))

    def get_analysis(self, namespace):
        """
        Return the the given analysis data under the the given name.

        Args:
            namespace (str): The  model namespace / pipeline module name.

        Returns:
            dict: An arbitrary dictionary containing predictions, content, etc.

        """
        name = getattr(namespace, "namespace", "analysis.{}".format(namespace))
        return self.get_attr(name)

    def get_predicted_labels(self, namespace, min_score=None):
        """
        Get all predictions made by the given label prediction module. If no
        label predictions are present, returns None.

        Args:
            namespace (str): The analysis namespace, example 'boonai-label-detection'.
            min_score (float): Filter results by a minimum score.

        Returns:
            list: A list of dictionaries containing the predictions

        """
        name = getattr(namespace, "namespace", "analysis.{}".format(namespace))
        predictions = self.get_attr(f'{name}.predictions')
        if not predictions:
            return None
        if min_score:
            return [pred for pred in predictions if pred['score'] >= min_score]
        else:
            return predictions

    def get_predicted_label(self, namespace, label):
        """
        Get a prediction made by the given label prediction module.  If no
        label predictions are present, returns None.

        Args:
            namespace (str): The model / module name that created the prediction.
            label (mixed): A label name or integer index of a prediction.

        Returns:
            dict: a prediction dict with a label, score, etc.
        """

        preds = self.get_predicted_labels(namespace)
        if not preds:
            return None

        if isinstance(label, str):
            preds = [pred for pred in preds if pred['label'] == label]
            label = 0

        try:
            return preds[label]
        except IndexError:
            return None

    def extend_list_attr(self, attr, items):
        """
        Adds the given items to the given attr. The attr must be a list or set.

        Args:
            attr (str): The name of the attribute
            items (:obj:`list` of :obj:`mixed`): A list of new elements.

        """
        items = as_collection(items)
        all_items = self.get_attr(attr)
        if all_items is None:
            all_items = set()
            self.set_attr(attr, all_items)
        try:
            all_items.update(items)
        except AttributeError:
            all_items.extend(items)

    def __set_attr(self, attr, value):
        """
        Handles setting an attribute value.

        Args:
            attr (str): The attribute name in dot notation format.  ex: 'foo.bar'
            value (mixed): The value for the particular attribute.
                Can be any json serializable type.
        """
        doc = self.document
        parts = attr.split(".")
        for k in parts[0:len(parts) - 1]:
            if k not in doc:
                doc[k] = {}
            doc = doc[k]
        if isinstance(value, dict):
            doc[parts[-1]] = value
        else:
            try:
                doc[parts[-1]] = value.for_json()
            except AttributeError:
                doc[parts[-1]] = value

    def __setitem__(self, field, value):
        self.set_attr(field, value)

    def __getitem__(self, field):
        return self.get_attr(field)


class FileImport:
    """
    An FileImport is used to import a new file and metadata into Boon AI.
    """

    def __init__(self, uri, transient=False, custom=None, page=None, label=None):
        """
        Construct an FileImport instance which can point to a remote URI.

        Args:
            uri (str): a URI locator to the file asset.
            transient (bool): File will be deleted after processing
            custom (dict): Values for custom metadata fields.
            page (int): The specific page to import if any.
            label (Label): An optional Label which will add the file to
                a Model training set.
        """
        super(FileImport, self).__init__()
        self.uri = uri
        self.custom = custom or {}
        self.page = page
        self.label = label
        self.tmp = self.__build_tmp_dict(transient)

    def __build_tmp_dict(self, transient):
        return {
            'transient': transient
        }

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "uri": self.uri,
            "custom": self.custom,
            "page": self.page,
            "label": self.label,
            "tmp": self.tmp,
        }

    def __setitem__(self, field, value):
        self.custom[field] = value

    def __getitem__(self, field):
        return self.custom[field]

    def __str__(self):
        return '<FileImport uri={}>'.format(self.uri)

    def __repr__(self):
        return '<FileImport uri="{}" object at {}>'.format(self.uri, hex(id(self)))


class FileUpload(FileImport):
    """
    FileUpload instances point to a local file that will be uploaded for analysis.
    """

    def __init__(self, path, transient=False, custom=None, page=None, label=None):
        """
        Create a new FileUpload instance.

        Args:
            path (str): A path to a file, the file must exist.
            transient (bool): File will be deleted after processing
            custom (dict): Values for pre-created custom metadata fields.
            page (int): The specific page to import if any.
            label (Label): An optional Label which will add the file to
                a Model training set.
        """
        super(FileUpload, self).__init__(
            os.path.normpath(os.path.abspath(path)), transient, custom, page, label)

        if not os.path.exists(path):
            raise ValueError('The path "{}" does not exist'.format(path))

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "uri": self.uri,
            "page": self.page,
            "label": self.label,
            "custom": self.custom,
            "tmp": self.tmp
        }

    def __str__(self):
        return '<FileUpload uri={}>'.format(self.uri)

    def __repr__(self):
        return '<FileUpload uri="{}" object at {}>'.format(self.uri, hex(id(self)))


class CsvFileImport:
    """
    The CsvFileImport class is used to import files and metadata in a a CSV file.
    """

    def __init__(self, path,
                 uri_column=0,
                 dataset=None,
                 label_column=None,
                 label_scope=LabelScope.TRAIN,
                 field_map=None,
                 max_assets=None,
                 header='auto',
                 delimiter=',',
                 quotechar='"'):
        """
        Create a new CsvFileImport instance.

        Args:
            path (str): The path to the CSV file.
            uri_column (int): The index of the column that contains an importable URI.
                Defaults to 0
            dataset (Dataset): A Dataset to assign the assets to.  Must also set label_index.
            label_column (int): The column that contains the label.
            label_scope (mixed): A label scope name, a LabelScope, a column index (int),
                a callable to convert a row value, or a float for dynamic selection.
            field_map (dict): A map of custom field names to index columns.
            max_assets (int): The maximum number of Assets to generate.
            header(mixed): 'auto' for autodetect, true for yes there is a header, false otherwise.
                Defaults to 'auto'
            delimiter (str): The CSV delimited, defaults to a comma.
            quotechar (str): The CSV special quote char, in the case a value contains the delimiter.
        """
        self.path = os.path.abspath(path)
        self.uri_column = uri_column
        self.dataset = dataset
        self.label_column = label_column
        self.label_scope = label_scope
        self.max_assets = max_assets
        self.delimiter = delimiter
        self.quotechar = quotechar
        self.field_map = field_map or {}
        self.header = header
        self.batch_size = 50

        if self.dataset and self.label_column is None or \
                self.label_column is not None and not self.dataset:
            raise ValueError("The dataset and label_index args must both be set")

    def __iter__(self):
        return CsvBatchGenerator(self).generate_batches()


class CsvBatchGenerator:
    """
    The CsvBatchGenerator generates batches of FileImport instance from a
    supplied CSV file.
    """
    def __init__(self, csvfile):
        """
        Create a new CsvBatchGenerator instance.

        Args:
            csvfile (CsvFileImport): The CsvFileImport object.
        """
        self.csv = csvfile

        self.file_queue = []
        """The queue of files we append to."""
        self.asset_count = 0
        """The number of assets processed thus far"""
        self.batch_size = csvfile.batch_size
        """The number of files in each batch"""
        self.label_counts = {}
        """Keeps track of the number of train labels for each class"""

    def generate_batches(self):
        """
        Generates all batches.

        Returns:
            list: A list of FileImport instances
        """
        with open(self.csv.path, 'r', newline='') as fp:
            if self.csv.header == "auto":
                has_header = csv.Sniffer().has_header(fp.read(1024))
                fp.seek(0)
            else:
                has_header = bool(self.csv.header)

            reader = csv.reader(fp,
                                delimiter=self.csv.delimiter,
                                quotechar=self.csv.quotechar)
            if has_header:
                next(reader)

            for row in reader:

                # Handle custom field extraction.
                custom = None
                if self.csv.field_map:
                    custom = {}
                    for k, v in self.csv.field_map.items():
                        if isinstance(v, (tuple, list)):
                            idx = v[0]
                            cast = v[1]
                            value = row[idx].strip()
                            if value:
                                if cast == 'int':
                                    custom[k] = int(value)
                                elif cast == 'float':
                                    custom[k] = float(value)
                                else:
                                    self.logger.warning(
                                        'Invalid cast type: {}, must be int or float'.format(v[1]))
                        else:
                            custom[k] = row[v].strip()

                # Handle the label if set.
                label = None
                if self.csv.dataset and self.csv.label_column is not None:
                    label_name = row[self.csv.label_column].strip()
                    scope = self.get_label_scope(self.csv.label_scope, label_name, row)
                    label = self.csv.dataset.make_label(label_name, scope=scope)

                # Handle the image URL
                # Check if line looks like a json/python array
                img_url = row[self.csv.uri_column].strip()
                if img_url.startswith("[") and img_url.endswith("]"):
                    uris = json.loads(img_url)
                else:
                    uris = [img_url]

                for uri in uris:
                    if not isinstance(uri, str):
                        continue
                self.file_queue.append(FileImport(uri, custom=custom, label=label))

                yield_size = self.get_yield_size()
                if yield_size > 0:
                    yield self.get_yield_results(yield_size)
                    if self.csv.max_assets and self.asset_count >= self.csv.max_assets:
                        return

            # Handle final batch
            yield_size = self.get_yield_size(force=True)
            if yield_size:
                yield self.get_yield_results(yield_size)
            else:
                return

    def get_yield_results(self, size):
        """
        Return an list of FileImport objects to yield and reset the queue.

        Args:
            size (int): The number of objects to retuurn

        Returns:
            list: The list of FileImport objects.
        """
        queue_copy = self.file_queue[0:size]
        self.asset_count += len(queue_copy)
        self.file_queue = []
        return queue_copy

    def get_yield_size(self, force=False):
        """
        Return the number of FileImport instances to yield, or 0 if the batch
        is not ready yet.

        Args:
            force (bool): Set to true to force a yield size.

        Returns:
            int: The number of FileImport objects to yield.
        """
        queue_size = len(self.file_queue)
        if not force and queue_size < self.batch_size:
            return 0
        elif self.csv.max_assets and self.csv.max_assets - self.asset_count < queue_size:
            return self.csv.max_assets - self.asset_count
        else:
            return queue_size

    def get_label_scope(self, label_scope, label_name, row):
        """
        Determine the label scope for a given row of the CSV file.  This can be accomplished
        a multitude of ways.

        Args:
            label_scope (mixed): A label scope name, a LabelScope, a column index (int),
                a callable to convert a row value, or a float for dynamic selection.
            label_name (str): The name of the label to find the scope for.
            row (list): The row of CSV values.

        Returns:
            LabelScope: The found label scope.
        """
        if isinstance(label_scope, str):
            return LabelScope[label_scope.upper()]
        elif isinstance(label_scope, LabelScope):
            return label_scope
        elif isinstance(label_scope, int):
            value = row[label_scope].upper()
            return LabelScope[value]
        elif isinstance(label_scope, float):
            count = self.label_counts.get(label_name, 0) + 1
            self.label_counts[label_name] = count
            if count == 1:
                return LabelScope.TEST
            elif count * label_scope >= 1.0:
                self.label_counts[label_name] = 0
                return LabelScope.TRAIN
            else:
                return LabelScope.TRAIN
        elif callable(label_scope):
            return label_scope(row)
        else:
            raise ValueError(f'Unable to determine label scope from ${label_scope}')


class Asset(DocumentMixin):
    """
    An Asset represents a single processed file.  Assets start out
    in the 'CREATED' state, which indicates they've been created by not processed.
    Once an asset has been processed and augmented with files created by various
    analysis modules, the Asset will move into the 'ANALYZED' state.
    """

    def __init__(self, data):
        super(Asset, self).__init__()
        if not data:
            raise ValueError("Error creating Asset instance, Assets must have an id.")
        self.id = data.get("id")
        self.document = data.get("document", {})
        self.score = data.get("score", 0)
        self.inner_hits = data.get("inner_hits", [])

    @staticmethod
    def from_hit(hit):
        """
        Converts an ElasticSearch hit into an Asset.

        Args:
            hit (dict): An raw ES document

        Returns:
            Asset: The Asset.
        """
        return Asset({
            'id': hit['_id'],
            'score': hit.get('_score', 0),
            'document': hit.get('_source', {}),
            'inner_hits': hit.get('inner_hits', [])})

    @property
    def uri(self):
        """
        The URI of the asset.

        Returns:
            str: The URI of the data.

        """
        return self.get_attr("source.path")

    @property
    def extension(self):
        """
        The file extension of the asset, lower cases.

        Returns:
            str: The file extension

        """
        return self.get_attr("source.extension").lower()

    @property
    def simhash(self):
        """
        Return the image similarity hash.

        Returns:
            str: The proxy image similarity hash.
        """
        return self.get_attr('analysis.boonai-image-similarity.simhash')

    def add_file(self, stored_file):
        """
        Adds the StoredFile record to the asset's list of associated files.

        Args:
            stored_file (StoredFile): A file that has been stored in Boon AI

        Returns:
            bool: True if the file was added to the list, False if it was a duplicate.

        """
        # Ensure the file doesn't already exist in the metadata
        if not self.get_files(id=stored_file.id):
            files = self.get_attr("files") or []
            files.append(stored_file._data)
            self.set_attr("files", files)
            return True
        return False

    def get_files(self, name=None, category=None, mimetype=None, extension=None,
                  id=None, attrs=None, attr_keys=None, sort_func=None):
        """
        Return all stored files associated with this asset.  Optionally
        filter the results.

        Args:
            name (str): The associated files name.
            category (str): The associated files category, eg proxy, backup, etc.
            mimetype (str): The mimetype must start with this string.
            extension: (str): The file name must have the given extension.
            attrs (dict): The file must have all of the given attributes.
            attr_keys: (list): A list of attribute keys that must be present.
            sort_func: (func): A lambda function for sorting the result.
        Returns:
            list of StoredFile: A list of Boon AI file records.

        """
        result = []
        files = self.get_attr("files") or []
        for fs in files:
            match = True
            if id and not any((item for item in as_collection(id)
                               if fs["id"] == item)):
                match = False
            if name and not any((item for item in as_collection(name)
                                 if fs["name"] == item)):
                match = False
            if category and not any((item for item in as_collection(category)
                                     if fs["category"] == item)):
                match = False
            if mimetype and not any((item for item in as_collection(mimetype)
                                     if fs["mimetype"].startswith(item))):
                match = False
            if extension and not any((item for item in as_collection(extension)
                                      if fs["name"].endswith("." + item))):
                match = False

            file_attrs = fs.get("attrs", {})
            if attr_keys:
                if not any(key in file_attrs for key in as_collection(attr_keys)):
                    match = False

            if attrs:
                for k, v in attrs.items():
                    if file_attrs.get(k) != v:
                        match = False
            if match:
                result.append(StoredFile(fs))

        if sort_func:
            result = sorted(result, key=sort_func)

        return result

    def get_thumbnail(self, level):
        """
        Return an thumbnail StoredFile record for the Asset. The level
        corresponds size of the thumbnail, 0 for the smallest, and
        up to N for the largest.  Levels 0,1,and 2 are smaller than
        the source media, level 3 or above  (if they exist) will
        be full resolution or higher images used for OCR purposes.

        To download the thumbnail call app.assets.download_file(stored_file)

        Args:
            level (int): The size level, 0 for smallest up to N.

        Returns:
            StoredFile: A StoredFile instance or None if no image proxies exist.
        """
        files = self.get_files(mimetype="image/", category="proxy",
                               sort_func=lambda f: f.attrs.get('width', 0))
        if not files:
            return None
        if level >= len(files):
            level = -1
        return files[level]

    def get_inner_hits(self, name):
        """
        Return any inner hits from a collapse query.

        Args:
            name (str): The inner hit name.

        Returns:
            list[Asset]:  A list of Assets.
        """
        try:
            return [Asset.from_hit(hit) for hit in self.inner_hits[name]['hits']['hits']]
        except KeyError:
            return []

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "id": self.id,
            "uri": self.get_attr("source.path"),
            "document": self.document,
            "page": self.get_attr("media.pageNumber"),
        }

    def __str__(self):
        return "<Asset id='{}'/>".format(self.id)

    def __repr__(self):
        return "<Asset id='{}' at {}/>".format(self.id, hex(id(self)))

    def __hash__(self):
        return hash(self.id)

    def __eq__(self, other):
        if not getattr(other, "id"):
            return False
        return other.id == self.id


class FileTypes:
    """
    A class for storing the supported file types.
    """

    videos = frozenset(['mov', 'mp4', 'mpg', 'mpeg', 'm4v', 'webm', 'ogv', 'ogg', 'mxf', 'avi'])
    """A set of supported video file formats."""

    images = frozenset(["bmp", "cin", "dpx", "gif", "jpg",
                        "jpeg", "exr", "png", "psd", "rla", "tif", "tiff",
                        "dcm", "rla"])
    """A set of supported image file formats."""

    documents = frozenset(['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'vsd', 'vsdx'])
    """A set of supported document file formats."""

    all = frozenset(videos.union(images).union(documents))

    """A set of all supported file formats."""

    @classmethod
    def supported(cls, path):
        try:
            ext = os.path.splitext(path)[1][1:]
            return ext in cls.all
        except Exception:
            return False

    @classmethod
    def resolve(cls, file_types):
        """
        Resolve a list of file extensions or types (images, documents, videos) to
        a supported list of extensions.

        Args:
            file_types (list): A list of file extensions, dot not included.

        Returns:
            list: The valid list of extensions from the given list

        """
        file_types = as_collection(file_types)
        if not file_types:
            return cls.all
        result = set()
        for file_type in file_types:
            if file_type in cls.all:
                result.add(file_type)
            else:
                exts = getattr(cls, file_type, None)
                if exts:
                    result.update(exts)

        return sorted(list(result))
