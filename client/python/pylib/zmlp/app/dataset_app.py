import json
import logging
import os

from ..entity import DataSet, DataSetType
from ..util import as_collection, as_id

logger = logging.getLogger(__name__)

__all__ = [
    'DataSetApp',
    'DataSetDownloader'
]


class DataSetApp:

    def __init__(self, app):
        self.app = app

    def create_dataset(self, name, type):
        """
        Create a new DataSet of the given type.  DataSets must have
        unique names. Once the type is set, it cannot be changed.

        Args:
            name (str): The name of the DataSet.
            type (DataSetType):The type of DataSet.

        Returns:
            DataSet: The new DataSet
        """
        req = {
            'name': name,
            'type': type.name
        }
        return DataSet(self.app.client.post('/api/v1/data-sets', req))

    def get_dataset(self, id):
        """
        Get a DataSet by its unique Id.

        Args:
            id (str): The DataSet or unique DataSet id.

        Returns:
            DataSet: The DataSet
        """
        return DataSet(self.app.client.get('/api/v1/data-sets/{}'.format(as_id(id))))

    def find_one_dataset(self, id=None, name=None, type=None):
        """
        Find a single DataSet.

        Args:
            id (mixed): An ID or collection of IDs to filter on.
            name (mixed): A name or collection of names to filter on.
            type: (mixed): A DataSet type or collection of types to filter on.

        Returns:
            DataSet: The matching DataSet

        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
        }
        return DataSet(self.app.client.post('/api/v3/data-sets/_find_one', body))

    def find_datasets(self, id=None, name=None, type=None, limit=None, sort=None):
        """
        Search for datasets.

        Args:
            id (str): An ID or collection of IDs to filter on.
            name (str): A name or collection of names to filter on.
            type: (str): A DataSet type or collection of types to filter on.
            limit: (int) Limit the number of results.
            sort: (list): A sort array, example: ["time_created:desc"]

        Returns:
            generator: A generator which will return matching DataSets when iterated.

        """
        body = {
            'names': as_collection(name),
            'ids': as_collection(id),
            'types': as_collection(type),
            'sort': sort
        }
        return self.app.client.iter_paged_results('/api/v1/data-sets/_search', body, limit, DataSet)

    def get_label_counts(self, dataset):
        """
        Get a dictionary of the labels and how many times they occur in the dataset.

        Args:
            dataset (dataset): The dataset or its unique Id.

        Returns:
            dict: a dictionary of label name to occurrence count.

        """
        return self.app.client.get('/api/v3/data-sets/{}/_label_counts'.format(as_id(dataset)))


class DataSetDownloader:
    """
    The DataSetDownloader class handles writing out the images in a
    DataSet to local disk for model training purposes.

    Multiple directory layouts are supported based on the DataSet type.

    Examples:

        # Label Detection Layout
        base_dir/flowers/set_train/daisy
        base_dir/flowers/set_train/rose
        base_dir/flowers/set_test/daisy
        base_dir/flowers/set_test/rose

        # Object Detection Layout is a COCO compatible layout
        base_dir/set_train/images/*
        base_dir/set_train/annotations.json
        base_dir/set_test/images/*
        base_dir/set_test/annotations.json
    """

    SET_TRAIN = "set_train"
    """Directoy name for training images"""

    SET_TEST = "set_test"
    """Directory name for test images"""

    def __init__(self, app, dataset, dst_dir, train_test_ratio=4):
        """
        Create a new DataSetDownloader.

        Args:
            app: (ZmlpApp): A ZmlpApp instance.
            dataset: (DataSet): A DataSet or unique DataSet ID.
            dst_dir (str): A destination directory to write the files into.
            train_test_ratio (int): The number of images in the training
                set for every image in the test set.
        """
        self.app = app
        self.dataset = app.datasets.get_dataset(dataset)
        self.dst_dir = dst_dir
        self.train_test_ratio = train_test_ratio

        self.labels = {}
        self.label_distrib = {}

        self.query = {
            'size': 32,
            '_source': ['labels', 'files'],
            'query': {
                'nested': {
                    'path': 'labels',
                    'query': {
                        'term': {'labels.dataSetId': self.dataset.id}
                    }
                }
            }
        }

        os.makedirs(self.dst_dir, exist_ok=True)

    def download(self, pool=None):
        """
        Downloads the files in the DataSet to local disk.

        Args:
            pool (multiprocessing.Pool): An optional Pool instance which can be used
                to download files in parallel.

        """
        if self.dataset.type == DataSetType.LABEL_DETECTION:
            self._build_label_detection_structure(pool)
        elif self.dataset.type == DataSetType.OBJECT_DETECTION:
            self._build_object_detection_structure(pool)
        else:
            raise ValueError("{} not supported by the DataSetDownloader".format(self.dataset.type))

    def _build_label_detection_structure(self, pool):

        self._setup_label_detection_base_dir()

        for num, asset in enumerate(self.app.assets.scroll_search(self.query, timeout='5m')):
            prx = asset.get_thumbnail(0)
            if not prx:
                logger.warning('{} did not have a suitable thumbnail'.format(asset))
                continue

            ds_labels = self._get_dataset_labels(asset)
            if not ds_labels:
                logger.warning('{} did not have any labels'.format(asset))
                continue

            label = ds_labels[0].get('label')
            if not label:
                logger.warning('{} was not labeled.'.format(asset))
                continue

            dir_name = self._get_image_set_type(label)
            dst_path = os.path.join(self.dst_dir, dir_name, label, prx.cache_id)
            os.makedirs(os.path.dirname(dst_path), exist_ok=True)

            logger.info('Downloading to {}'.format(dst_path))
            if pool:
                pool.apply_async(self.app.assets.download_file, args=(prx, dst_path))
            else:
                self.app.assets.download_file(prx, dst_path)

    def _build_object_detection_structure(self, pool=None):
        """
        Write a DataSet in a COCO object detection training structure.

        Args:
            pool (multiprocessing.Pool): A multi-processing pool for downloading really fast.

        Returns:
            str: A path to an annotation file.

        """

        self._setup_object_detection_base_dir()

        coco = CocoAnnotationFileBuilder()

        for image_id, asset in enumerate(self.app.assets.scroll_search(self.query, timeout='5m')):
            prx = asset.get_thumbnail(1)
            if not prx:
                logger.warning('{} did not have a suitable thumbnail'.format(asset))
                continue

            ds_labels = self._get_dataset_labels(asset)
            if not ds_labels:
                logger.warning('{} did not have any labels'.format(asset))
                continue

            for label in ds_labels:

                set_type = self._get_image_set_type(label['label'])
                dst_path = os.path.join(self.dst_dir, set_type, 'images', prx.cache_id)
                if not os.path.exists(dst_path):
                    self._download_file(prx, dst_path, pool)

                image = {
                    'file_name': dst_path,
                    'height': prx.attrs['height'],
                    'width': prx.attrs['width']
                }

                category = {
                    'supercategory': 'none',
                    'name': label['label']
                }

                bbox, area = self._zvi_to_cocos_bbox(prx, label['bbox'])
                annotation = {
                    'bbox': bbox,
                    'segmentation': [],
                    'ignore': 0,
                    'area': area,
                    'iscrowd': 0
                }

                if set_type == self.SET_TRAIN:
                    coco.add_to_training_set(image, category, annotation)
                else:
                    coco.add_to_test_set(image, category, annotation)

        # Write out the annotations files.
        with open(os.path.join(self.dst_dir, self.SET_TRAIN, "annotations.json"), "w") as fp:
            logger.debug("Writing training set annotations to {}".format(fp.name))
            json.dump(coco.get_training_annotations(), fp)

        with open(os.path.join(self.dst_dir, self.SET_TEST, "annotations.json"), "w") as fp:
            logger.debug("Writing test set annotations to {}".format(fp.name))
            json.dump(coco.get_test_annotations(), fp)

    def _zvi_to_cocos_bbox(self, prx, bbox):
        """
        Converts a ZVI bbox to a COCOs bbox.  The format is x, y, width, height.

        Args:
            prx (StoredFile): A StoredFile containing a proxy image.
            bbox (list): A ZVI bbox.

        Returns:
            list[float]: A COCOs style bbox.
        """
        total_width = prx.attrs['width']
        total_height = prx.attrs['height']
        pt = total_width * bbox[0], total_height * bbox[1]

        new_bbox = [
            int(pt[0]),
            int(pt[1]),
            abs(pt[0] - int((total_width * bbox[2]))),
            abs(pt[0] - int((total_height * bbox[3])))
        ]
        area = new_bbox[1] * new_bbox[2]
        return new_bbox, area

    def _download_file(self, prx, dst_path, pool=None):
        if pool:
            pool.apply_async(self.app.assets.download_file, args=(prx, dst_path))
        else:
            self.app.assets.download_file(prx, dst_path)

    def _setup_label_detection_base_dir(self):
        """
        Sets up a directory structure for storing the files in the DataSet.

        The structure is basically:
            set_train/<label>/<img file>
            set_test/<label>/<img file>
        """
        self.labels = self.app.datasets.get_label_counts(self.dataset.id)

        # This is layout #1, we need to add darknet layout for object detection.
        dirs = (self.SET_TRAIN, self.SET_TEST)
        for set_name in dirs:
            os.makedirs('{}/{}'.format(self.dst_dir, set_name), exist_ok=True)
            for label in self.labels.keys():
                os.makedirs(os.path.join(self.dst_dir, set_name, label), exist_ok=True)

        logger.info('DataSetDownloader setup, using {} labels'.format(len(self.labels)))

    def _setup_object_detection_base_dir(self):
        dirs = (self.SET_TRAIN, self.SET_TEST)
        for set_name in dirs:
            os.makedirs('{}/{}/images'.format(self.dst_dir, set_name), exist_ok=True)

    def _get_image_set_type(self, label):
        """
        Using the train_ratio property, determine if the current label
        would be in the training set or test set.

        Args:
            label (str): The label name.

        Returns:
            str: Either set_test or set_test, depending on the train_test_ratio property.

        """
        value = self.label_distrib.get(label, -1) + 1
        self.label_distrib[label] = value
        if value % self.train_test_ratio == 0:
            return self.SET_TEST
        else:
            return self.SET_TRAIN

    def _get_dataset_labels(self, asset):
        """
        Get the current dataset label for the given asset.

        Args:
            asset (Asset): The asset to check.

        Returns:
            list[dict]: The labels in this DataSet

        """
        ds_labels = asset.get_attr('labels')
        if not ds_labels:
            return []
        result = []
        for ds_label in ds_labels:
            if ds_label.get('dataSetId') == self.dataset.id:
                result.append(ds_label)
        return result


class CocoAnnotationFileBuilder:
    """
    CocoAnnotationFileBuilder manages building a COCO annotations file for both
    a training set and test set.
    """

    def __init__(self):
        self.train_set = {
            "output": {
                "type": "instances",
                "images": [],
                "annotations": [],
                "categories": []
            },
            "img_set": {},
            "cat_set": {}
        }

        self.test_set = {
            "output": {
                "type": "instances",
                "images": [],
                "annotations": [],
                "categories": []
            },
            "img_set": {},
            "cat_set": {}
        }

    def add_to_training_set(self, img, cat, annotation):
        """
        Add the image, category and annotation to the training set.

        Args:
            img (dict): A COCO image dict.
            cat (dict): A COCO categor dict.
            annotation: (dict): A COCO annotation dict.
        """
        self._add_to_set(self.train_set, img, cat, annotation)

    def add_to_test_set(self, img, cat, annotation):
        """
        Add the image, category and annotation to the test set.

        Args:
            img (dict): A COCO image dict.
            cat (dict): A COCO categor dict.
            annotation: (dict): A COCO annotation dict.

        """
        self._add_to_set(self.test_set, img, cat, annotation)

    def _add_to_set(self, dataset, img, cat, annotation):
        """
        Add the image, category and annotation to the given set.

        Args:
            dataset (dict): The set we're building.
            img (dict): A COCO image dict.
            cat (dict): A COCO categor dict.
            annotation: (dict): A COCO annotation dict.
        """
        img_idmap = dataset['img_set']
        cat_idmap = dataset['cat_set']
        output = dataset['output']
        annots = output['annotations']

        img['id'] = img_idmap.get(img['file_name'], len(img_idmap))
        cat['id'] = cat_idmap.get(cat['name'], len(cat_idmap))
        annotation['id'] = len(annots)
        annotation['category_id'] = cat['id']
        annotation['image_id'] = img['id']

        if img['file_name'] not in img_idmap:
            img_idmap[img['file_name']] = img['id']
            output['images'].append(img)

        if cat['name'] not in cat_idmap:
            cat_idmap[cat['name']] = cat['id']
            output['categories'].append(cat)

        output['annotations'].append(annotation)

    def get_training_annotations(self):
        """
        Return a structure suitable for a COCO annotations file.

        Returns:
            dict: The training annoations.=
        """
        return self.train_set['output']

    def get_test_annotations(self):
        """
        Return a structure suitable for a COCO annotations file.

        Returns:
            dict: The test annoations.
        """
        return self.test_set['output']
