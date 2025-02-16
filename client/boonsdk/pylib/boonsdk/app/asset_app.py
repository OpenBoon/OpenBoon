import datetime
import os
from collections import namedtuple

import requests
from deprecation import deprecated

from ..entity import Asset, StoredFile, FileUpload, FileTypes, Job, VideoClip, CsvFileImport
from ..filters import apply_search_filters
from ..search import AssetSearchResult, AssetSearchScroller, SimilarityQuery, SearchScroller
from ..util import as_collection, as_id_collection, as_id


class AssetApp:
    """Methods for managing Assets"""

    def __init__(self, app):
        self.app = app

    def batch_import_files(self, files, modules=None, job_name=None):
        """
        Import a list of FileImport instances.

        Args:
            files (list of FileImport): The list of files to import as Assets.
            modules (list): A list of Pipeline Modules to apply to the data.

        Returns:
            dict: A dictionary containing failed files and created asset ids.
        """
        body = {
            'assets': as_collection(files),
            'modules': modules,
            'jobName': job_name
        }
        return self.app.client.post('/api/v3/assets/_batch_create', body)

    def import_csv(self, csvfile, modules=None, job_name=None):
        """
        Import files list in a CSV file.

        Args:
            csvfile (CsvFileImport): A CsvFileImport to describe the file.
            modules (list): The list of modules to apply.
            job_name (str): A job name to import the CSV, will default to a generated name.
        Returns:
            dict: The last response from the batch import operation.
        """
        if not isinstance(csvfile, CsvFileImport):
            raise ValueError("The csvfile argument must be an instance of CsvFileImport")
        if not job_name:
            job_name = 'CSV import of "{}" at {}'.format(
                os.path.basename(csvfile.path), datetime.datetime.now())

        results = {'errors': 0, 'created': 0, 'exists': 0}
        for batch in csvfile:
            result = self.batch_import_files(batch, modules=modules, job_name=job_name)
            results['errors'] += len(result.get('failed', []))
            results['created'] += len(result.get('created', []))
            results['exists'] += len(result.get('exists', []))
            results['jobId'] = result.get('jobId')
        return results

    def analyze_file(self, iostream, modules):
        """
        Analyze an Image file stream with the given modules.  Only Boon AI models
        and custom models currently supported.

        Args:
            iostream (IOBase): A file handle or blob of bytes representing an image.
            modules (list): A list of modules to apply.
        Returns:
            list: A list of dicts representing predictions.
        """
        if not modules:
            raise ValueError('At least 1 module is required')

        qstr = ','.join(modules)
        return Asset(self.app.client.send_data(
            f'/ml/v1/modules/apply-to-file?modules={qstr}', iostream))

    def batch_upload_files(self, files, modules=None, job_name=None):
        """
        Batch upload a list of files and return a structure which contains
        an ES bulk response object, a list of failed file paths, a list of created
        asset Ids, and a processing jobId.

        Args:
            files (list of FileUpload):
            modules (list): A list of Pipeline Modules to apply to the data.
            job_name (str): The job name for the batch.

        Returns:
            dict: A dictionary containing failed files and created asset ids.
        """
        files = as_collection(files)
        file_paths = [f.uri for f in files]
        body = {
            'assets': files,
            'modules': modules,
            'jobName': job_name
        }
        return self.app.client.upload_files('/api/v3/assets/_batch_upload',
                                            file_paths, body)

    def batch_upload_directory(self, path, file_types=None,
                               batch_size=50, max_batches=None, modules=None,
                               label=None, callback=None):
        """
        Recursively upload all files in the given directory path.

        This method takes an optional callback function which takes two
        arguments, files and response.  This callback is called for
        each batch of files submitted.

        Args:
            path (str): A file path to a directory.
            file_types (list): a list of file extensions and/or
                categories(documents, images, videos)
            batch_size (int) The number of files to upload per batch.
            max_batches (int) The max number of batches to upload.
            modules (list): An array of modules to apply to the files.
            label (Label): A label to add to the Assets.
            callback (func): A function to call for every batch

        Returns:
            dict: A dictionary containing batch operation counters.
        """
        batch = []
        batch_count = 0
        max_batches_reached = False

        totals = {
            'file_count': 0,
            'file_size': 0,
            'batch_count': 0,
        }

        def process_batch():
            totals['batch_count'] += 1
            totals['file_count'] += len(batch)
            totals['file_size'] += sum([os.path.getsize(f) for f in batch])

            rsp = self.batch_upload_files(
                [FileUpload(f, label=label) for f in batch], modules)
            if callback:
                callback(batch.copy(), rsp)
            batch.clear()

        file_types = FileTypes.resolve(file_types)
        for root, dirs, files in os.walk(path):
            for fname in files:
                if fname.startswith('.'):
                    continue
                _, ext = os.path.splitext(fname)
                if not ext:
                    continue
                if ext[1:].lower() not in file_types:
                    continue
                batch.append(os.path.abspath(os.path.join(root, fname)))
                if len(batch) >= batch_size:
                    process_batch()
                    batch_count += 1
                    if max_batches and batch_count >= max_batches:
                        max_batches_reached = True
                        break

        if batch and not max_batches_reached:
            process_batch()

        return totals

    def delete_asset(self, asset):
        """
        Delete the given asset.

        Args:
            asset (mixed): unique Id or Asset instance.

        Returns:
            bool: True if the asset was deleted.

        """
        asset_id = as_id(asset)
        return self.app.client.delete('/api/v3/assets/{}'.format(asset_id))['success']

    def batch_delete_assets(self, assets):
        """
        Batch delete the given list of Assets or asset ids.

        Args:
            assets (list): A list of Assets or unique asset ids.

        Returns:
            dict: A dictionary containing deleted and errored asset Ids.
        """
        body = {
            'assetIds': as_id_collection(assets)
        }
        return self.app.client.delete('/api/v3/assets/_batch_delete', body)

    def search(self, search=None, fetch_source=True, filters=None):
        """
        Perform an asset search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute.
            fetch_source (bool): If true, the full JSON document for each asset is returned.
            filters (list): A list of additional search filters.
        Returns:
            AssetSearchResult - an AssetSearchResult instance.
        """
        if not fetch_source:
            search['_source'] = False
        apply_search_filters(search, filters)
        return AssetSearchResult(self.app, search)

    def scroll_search(self, search=None, timeout="1m", filters=None):
        """
        Perform an asset scrolled search using the ElasticSearch query DSL.

        See Also:
            For search/query format.
            https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html

        Args:
            search (dict): The ElasticSearch search to execute
            timeout (str): The scroll timeout.  Defaults to 1 minute.
            filters (list): A list of Asset search filters.
        Returns:
            AssetSearchScroll - an AssetSearchScroller instance which is a generator
                by nature.

        """
        apply_search_filters(search, filters)
        return AssetSearchScroller(self.app, search, timeout)

    def reprocess_search(self, search, modules):
        """
        Reprocess the given search with the supplied modules.

        Args:
            search (dict): An ElasticSearch search.
            modules (list): A list of module names to apply.

        Returns:
            dict: Contains a Job and the number of assets to be processed.
        """
        body = {
            'search': search,
            'modules': modules
        }
        rsp = self.app.client.post('/api/v3/assets/_search/reprocess', body)
        return ReprocessSearchResponse(rsp['assetCount'], Job(rsp['job']))

    def label_search(self, search, dataset, label, max_assets=10000, test_ratio=0.0):
        """
        Label up to 10000 assets in a given search.

        Args:
            search (dict): An ElasticSearch search.
            dataset (Dataset): The Dataset to add the assets to.
            max_assets (int): The maximum # of assets to label.
            test_ratio (float): A number between 0-1 that defines the percentage of assets
                to label as test vs train.  For example a value of .2 would ensure at least
                20% of the assets would be labeled as test assets.

        Returns:
            dict: A dict with counts for total, test, train, and duplicates.
        """
        if test_ratio < 0.0 or test_ratio > 1.0:
            raise ValueError('The test_ratio must be between 0 ad 1')

        body = {
            'search': search,
            'datasetId': as_id(dataset),
            'label': label,
            'maxAssets': max_assets,
            'testRatio': test_ratio
        }
        return self.app.client.put('/api/v3/assets/_batch_label_by_search', body)

    def scroll_search_clips(self, asset, search=None, timeout="1m"):
        """
        Scroll through clips for given asset using the ElasticSearch query DSL.

        Args:
            asset (Asset): The asset or unique AssetId.
            search (dict): The ElasticSearch search to execute
            timeout (str): The scroll timeout.  Defaults to 1 minute.

        Returns:
            SearchScroller  a clip scroller instance for generating VideoClips.

        """
        asset_id = as_id(asset)
        return SearchScroller(
            VideoClip, f'/api/v3/assets/{asset_id}/clips/_search', self.app, search, timeout
        )

    def reprocess_assets(self, assets, modules):
        """
        Reprocess the given array of assets with the given modules.

        Args:
            assets (list): A list of Assets or asset unique Ids.
            modules (list): A list of Pipeline module names or ides.

        Returns:
            Job: The job responsible for processing the assets.
        """
        asset_ids = [getattr(asset, 'id', asset) for asset in as_collection(assets)]
        body = {
            'search': {
                'query': {
                    'terms': {
                        '_id': asset_ids
                    }
                }
            },
            'modules': as_collection(modules)
        }

        return self.app.client.post('/api/v3/assets/_search/reprocess', body)

    def get_asset(self, id):
        """
        Return the asset with the given unique Id.

        Args:
            id (str): The unique ID of the asset.

        Returns:
            Asset: The Asset
        """
        return Asset(self.app.client.get('/api/v3/assets/{}'.format(id)))

    def batch_add_labels(self, labels):
        """
        Add up to 1000 labels in a single request using a structure
        of dict[assetId -> Label].  This function allows you to add labels to
        an arbitrary set of assets.

        Args:
            labels (dict): A dictionary of AssetId to Label

        Returns:
            dict: A status dictionary
        """
        if not isinstance(labels, dict):
            raise ValueError('The labels argument must be a dictionary')
        ids = as_id_collection(list(labels.keys()))
        body = {
            'add': dict([(asset_id, labels[asset_id]) for asset_id in ids])
        }
        return self.app.client.put('/api/v4/assets/_batch_update_labels', body)

    def batch_remove_labels(self, labels):
        """
        Remove up to 1000 labels in a single request using a structure
        of dict[assetId -> Label]. This function allows you to remove labels
        to an arbitrary set of assets.

        Args:
            labels (dict): A dictionary of AssetId to Label

        Returns:
            dict: A status dictionary

        """
        if not isinstance(labels, dict):
            raise ValueError('The labels argument must be a dictionary')
        ids = as_id_collection(list(labels.keys()))
        body = {
            'remove': dict([(a, labels[a]) for a in ids])
        }
        return self.app.client.put('/api/v4/assets/_batch_update_labels', body)

    def batch_update_labels(self, assets, add_label=None, remove_label=None):
        """
        Add and/or remove a label from an a label from a collection of Assets.

        Args:
            assets (mixed): An Asset, asset ID, or a list of either type.
            add_label (Label): A Label or list of Label to add.
            remove_label (Label): A Label or list of Label to remove.
        Returns:
            dict: An request status dict

        """
        ids = as_id_collection(assets)
        body = {}
        if add_label:
            body['add'] = dict([(a, add_label) for a in ids])
        if remove_label:
            body['remove'] = dict([(a, remove_label) for a in ids])
        if not body:
            raise ValueError('Must pass at least and add_labels or remove_labels argument')
        return self.app.client.put('/api/v4/assets/_batch_update_labels', body)

    @deprecated(deprecated_in="1.4", removed_in="1.5", details="Use batch_update_labels() instead")
    def update_labels(self, assets, add_labels=None, remove_labels=None):
        """
        Update the Labels on the given array of assets.

        Args:
            assets (mixed): An Asset, asset ID, or a list of either type.
            add_labels (list[Label]): A Label or list of Label to add.
            remove_labels (list[Label]): A Label or list of Label to remove.
        Returns:
            dict: An request status dict

        """
        ids = as_id_collection(assets)
        body = {}
        if add_labels:
            body['add'] = dict([(a, as_collection(add_labels)) for a in ids])
        if remove_labels:
            body['remove'] = dict([(a, as_collection(remove_labels)) for a in ids])
        if not body:
            raise ValueError('Must pass at least and add_labels or remove_labels argument')
        return self.app.client.put('/api/v3/assets/_batch_update_labels', body)

    def set_field_values(self, asset, values):
        """
        Set the values of custom metadata fields.

        Args:
            asset (Asset): The asset or unique Asset id.
            values (dict): A dictionary of values keyed on the field path. (custom.foo)

        Returns:
            dict: A status dictionary with failures or success
        """
        body = {
            'update': {
                as_id(asset): values
            }
        }
        return self.app.client.put('/api/v3/assets/_batch_update_custom_fields', body)

    def batch_update_custom_fields(self, update):
        """
        Set the values of custom metadata fields.

        Examples:
            { \
                "asset-id1": {"shoe": "nike"}, \
                "asset-id2": {"country": "New Zealand"} \
            }

        Args:
            update (dict): A dict o dicts which describe the
        Returns:
            dict: A status dictionary with failures or success
        """
        body = {
            'update': update
        }
        return self.app.client.put('/api/v3/assets/_batch_update_custom_fields', body)

    def download_file(self, stored_file, dst_file=None):
        """
        Download given file and store results in memory, or optionally
        a destination file.  The stored_file ID can be specified as
        either a string like "assets/<id>/proxy/image_450x360.jpg"
        or a StoredFile instance can be used.

        Args:
            stored_file (mixed): The StoredFile instance or its ID.
            dst_file (str): An optional destination file path.

        Returns:
            io.BytesIO instance containing the binary data or if
                a destination path was provided the size of the
                file is returned.

        """
        return self.app.client.download_file(stored_file, dst_file)

    def stream_file(self, stored_file, chunk_size=1024):
        """
        Streams a file by iteratively returning chunks of the file using a generator. This
        can be useful when developing web applications and a full download of the file
        before continuing is not necessary.

        Args:
            stored_file (mixed): The StoredFile instance or its ID.
            chunk_size (int): The byte sizes of each requesting chunk. Defaults to 1024.

        Yields:
            generator (File-like Object): Content of the file.

        """
        if isinstance(stored_file, str):
            path = stored_file
        elif isinstance(stored_file, StoredFile):
            path = stored_file.id
        else:
            raise ValueError('stored_file must be a string or StoredFile instance')

        url = self.app.client.get_url('/api/v3/files/_stream/{}'.format(path))
        response = requests.get(url, verify=self.app.client.verify,
                                headers=self.app.client.headers(), stream=True)

        for block in response.iter_content(chunk_size):
            yield block

    def get_sim_hashes(self, images):
        """
        Return a similarity hash for the given array of images.

        Args:
            images (mixed): Can be an file handle (opened with 'rb'), or
                path to a file.
        Returns:
            list of str: A list of similarity hashes.

        """
        return self.app.client.upload_files('/ml/v1/sim-hash',
                                            as_collection(images), body=None)

    def get_sim_query(self, images, min_score=0.75):
        """
        Analyze the given image files and return a SimilarityQuery which
        can be used in a search.

        Args:
            images (mixed): Can be an file handle (opened with 'rb'), or
                path to a file.
            min_score (float): A float between, the higher the value the more similar
                the results.  Defaults to 0.75

        Returns:
            SimilarityQuery: A configured SimilarityQuery
        """
        return SimilarityQuery(self.get_sim_hashes(images), min_score)

    def apply_modules(self, asset, modules, index=True):
        """
        Apply a list of modules to a single Asset and return the new state of
        the Asset.  If the 'save' argument is true (default), the resulting asset will be
        saved back to your index.

        Args:
            asset (Asset): The Asset or the asset's unique ID.
            modules (list): A list of modules to apply, there are some limitations.
            index (bool): Index the results.

        Returns:
            The Asset with additional modules added.
        """
        body = {
            'assetId': as_id(asset),
            'modules': as_collection(modules),
            'index': index
        }
        return Asset(self.app.client.post('/ml/v1/modules/apply-to-asset', body))

    def set_languages(self, asset, languages):
        """
        Set the languages for the Asset.  This property is for various types
        of processing where knowing the language is important for accurate metadata.
        Setting multiple languages is ok.  If no language is set, BoonAI will try
        to auto-detect it from the media.

        Args:
            asset (Asset): The Asset or the asset's unique ID.
            languages (list): A list of BCP-47 language codes. (ex: en-US)
        Returns:
            dict: A update status dict.
        """
        asset_id = as_id(asset)
        body = as_collection(languages)
        return self.app.client.put(f'/api/v3/assets/{asset_id}/_set_languages', body)


"""
A named tuple to define a ReprocessSearchResponse
"""
ReprocessSearchResponse = namedtuple('ReprocessSearchResponse', ["asset_count", "job"])
