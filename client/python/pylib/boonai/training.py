"""Classes to support training with Models"""
import os
import logging
import json

logger = logging.getLogger(__name__)

__all__ = [
    'TrainingSetDownloader'
]


class TrainingSetDownloader:
    """
    The TrainingSetDownloader class handles writing out the images labeled
    for model training to local disk.  The Assets are automatically sorted
    into train and validation sets.

    Multiple directory layouts are supported based on the Model type.

    Examples:

        # Label Detection Layout
        base_dir/flowers/set_validate/daisy
        base_dir/flowers/set_validate/rose
        base_dir/flowers/set_validate/daisy
        base_dir/flowers/set_validate/rose

        # Object Detection Layout is a COCO compatible layout
        base_dir/set_train/images/*
        base_dir/set_train/annotations.json
        base_dir/set_test/images/*
        base_dir/set_test/annotations.json
    """

    SET_TRAIN = "train"
    """Directory name for training images"""

    SET_VALIDATION = "validate"
    """Directory name for test images"""

    def __init__(self, app,  model, style, dst_dir, validation_split=0.2):
        """
        Create a new TrainingImageDownloader.

        Args:
            app: (ZmlpApp): A ZmlpApp instance.
            model: (Model): A Model or unique Model ID.
            style: (str): The output style: labels_std, objects_keras, objects_coco
            dst_dir (str): A destination directory to write the files into.
            validation_split (float): The number of images in the training
                set for every image in the validation set.
        """
        self.app = app
        self.model = app.models.get_model(model)
        self.style = style
        self.dst_dir = dst_dir
        self.validation_split = validation_split

        self.labels = {}
        self.label_distrib = {}

        self.query = {
            'size': 64,
            '_source': ['labels', 'files'],
            'query': {
                'nested': {
                    'path': 'labels',
                    'query': {
                        'bool': {
                            'must': [
                                {'term': {'labels.modelId': self.model.id}},
                                {'term': {'labels.scope': 'TRAIN'}}
                            ]
                        }
                    }
                }
            }
        }

        os.makedirs(self.dst_dir, exist_ok=True)

    def build(self, pool=None):
        """
        Downloads the files labeled for training a Model to local disk.

        Args:
            labels_std, objects_keras, objects_coco
            pool (multiprocessing.Pool): An optional Pool instance which can be used
                to download files in parallel.

        """
        if self.style == 'labels-standard':
            self._build_labels_std_format(pool)
        elif self.style == 'objects_coco':
            self._build_objects_coco_format(pool)
        elif self.style == 'objects_keras':
            self._build_objects_keras_format(pool)
        else:
            raise ValueError('{} not supported by the TrainingSetDownloader'.format(format))

    def _build_labels_std_format(self, pool):

        self._setup_labels_std_base_dir()

        for num, asset in enumerate(self.app.assets.scroll_search(self.query, timeout='5m')):
            prx = asset.get_thumbnail(0)
            if not prx:
                logger.warning('{} did not have a suitable thumbnail'.format(asset))
                continue

            ds_labels = self._get_labels(asset)
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

    def _build_objects_coco_format(self, pool=None):
        """
        Write a labeled assets in a COCO object detection training structure.

        Args:
            pool (multiprocessing.Pool): A multi-processing pool for downloading really fast.

        Returns:
            str: A path to an annotation file.

        """

        self._setup_objects_coco_base_dir()

        coco = CocoAnnotationFileBuilder()

        for image_id, asset in enumerate(self.app.assets.scroll_search(self.query, timeout='5m')):
            prx = asset.get_thumbnail(1)
            if not prx:
                logger.warning('{} did not have a suitable thumbnail'.format(asset))
                continue

            ds_labels = self._get_labels(asset)
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
                    coco.add_to_validation_set(image, category, annotation)

        # Write out the annotations files.
        with open(os.path.join(self.dst_dir, self.SET_TRAIN, "annotations.json"), "w") as fp:
            logger.debug("Writing training set annotations to {}".format(fp.name))
            json.dump(coco.get_training_annotations(), fp)

        with open(os.path.join(self.dst_dir, self.SET_VALIDATION, "annotations.json"), "w") as fp:
            logger.debug("Writing test set annotations to {}".format(fp.name))
            json.dump(coco.get_validation_annotations(), fp)

    def _build_objects_keras_format(self, pool=None):
        self._setup_objects_keras_base_dir()

        fp_train = open(os.path.join(self.dst_dir, self.SET_TRAIN, "annotations.csv"), "w")
        fp_test = open(os.path.join(self.dst_dir, self.SET_VALIDATION, "annotations.csv"), "w")
        unique_labels = set()

        try:
            search = self.app.assets.scroll_search(self.query, timeout='5m')
            for image_id, asset in enumerate(search):
                prx = asset.get_thumbnail(1)
                if not prx:
                    logger.warning('{} did not have a suitable thumbnail'.format(asset))
                    continue

                ds_labels = self._get_labels(asset)
                if not ds_labels:
                    logger.warning('{} did not have any labels'.format(asset))
                    continue

                for label in ds_labels:
                    unique_labels.add(label['label'])
                    set_type = self._get_image_set_type(label['label'])
                    dst_path = os.path.join(self.dst_dir, set_type, 'images', prx.cache_id)
                    if not os.path.exists(dst_path):
                        self._download_file(prx, dst_path, pool)
                    line = [
                        dst_path
                    ]
                    line.extend([str(point) for point in
                                 self._zvi_to_keras_bbox(prx, label['bbox'])])
                    line.append(label['label'])
                    str_line = "{}\n".format(",".join(line))
                    if set_type == self.SET_TRAIN:
                        fp_train.write(str_line)
                    else:
                        fp_test.write(str_line)
        finally:
            fp_train.close()
            fp_test.close()

        with open(os.path.join(self.dst_dir, "classes.csv"), "w") as fp_classes:
            for idx, cls in enumerate(sorted(unique_labels)):
                fp_classes.write("{},{}\n".format(cls, idx))

    def _zvi_to_keras_bbox(self, prx, bbox):
        total_width = prx.attrs['width']
        total_height = prx.attrs['height']
        return [int(total_width * bbox[0]),
                int(total_height * bbox[1]),
                int(total_width * bbox[2]),
                int(total_height * bbox[3])]

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
            int(abs(pt[0] - (total_width * bbox[2]))),
            int(abs(pt[0] - (total_height * bbox[3])))
        ]
        area = (new_bbox[2] - new_bbox[0]) * (new_bbox[3] - new_bbox[1])
        return new_bbox, area

    def _download_file(self, prx, dst_path, pool=None):
        if pool:
            pool.apply_async(self.app.assets.download_file, args=(prx, dst_path))
        else:
            self.app.assets.download_file(prx, dst_path)

    def _setup_labels_std_base_dir(self):
        """
        Sets up a directory structure for storing files used to train a model..

        The structure is basically:
            train/<label>/<img file>
            validate/<label>/<img file>
        """
        self.labels = self.app.models.get_label_counts(self.model)

        # This is layout #1, we need to add darknet layout for object detection.
        dirs = (self.SET_TRAIN, self.SET_VALIDATION)
        for set_name in dirs:
            os.makedirs('{}/{}'.format(self.dst_dir, set_name), exist_ok=True)
            for label in self.labels.keys():
                os.makedirs(os.path.join(self.dst_dir, set_name, label), exist_ok=True)

        logger.info('TrainingSetDownloader setup, using {} labels'.format(len(self.labels)))

    def _setup_objects_coco_base_dir(self):
        dirs = (self.SET_TRAIN, self.SET_VALIDATION)
        for set_name in dirs:
            os.makedirs(os.path.join(self.dst_dir, set_name, 'images'), exist_ok=True)

    def _setup_objects_keras_base_dir(self):
        dirs = (self.SET_TRAIN, self.SET_VALIDATION)
        for set_name in dirs:
            os.makedirs(os.path.join(self.dst_dir, set_name, 'images'), exist_ok=True)

    def _get_image_set_type(self, label):
        """
        Using the validation_split property, determine if the current label
        would be in the training set or validation set.

        Args:
            label (str): The label name.

        Returns:
            str: Either 'validate' or 'train', depending on the validation_split property.

        """
        # Everything is in the training set.
        if self.validation_split <= 0.0:
            return self.SET_TRAIN

        ratio = int(1.0 / self.validation_split)
        value = self.label_distrib.get(label, 0) + 1
        self.label_distrib[label] = value

        if value % ratio == 0:
            return self.SET_VALIDATION
        else:
            return self.SET_TRAIN

    def _get_labels(self, asset):
        """
        Get the current model label for the given asset.

        Args:
            asset (Asset): The asset to check.

        Returns:
            list[dict]: The labels for training a model.

        """
        ds_labels = asset.get_attr('labels')
        if not ds_labels:
            return []
        result = []
        for ds_label in ds_labels:
            if ds_label.get('modelId') == self.model.id:
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

        self.validation_set = {
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
            cat (dict): A COCO category dict.
            annotation: (dict): A COCO annotation dict.
        """
        self._add_to_set(self.train_set, img, cat, annotation)

    def add_to_validation_set(self, img, cat, annotation):
        """
        Add the image, category and annotation to the test set.

        Args:
            img (dict): A COCO image dict.
            cat (dict): A COCO category dict.
            annotation: (dict): A COCO annotation dict.

        """
        self._add_to_set(self.validation_set, img, cat, annotation)

    def _add_to_set(self, dataset, img, cat, annotation):
        """
        Add the image, category and annotation to the given set.

        Args:
            dataset (dict): The set we're building.
            img (dict): A COCO image dict.
            cat (dict): A COCO category dict.
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

    def get_validation_annotations(self):
        """
        Return a structure suitable for a COCO annotations file.

        Returns:
            dict: The test annoations.
        """
        return self.validation_set['output']
