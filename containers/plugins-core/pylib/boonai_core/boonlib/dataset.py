import io

from boonflow import AssetProcessor, Argument
from boonsdk.util import to_json


class ExportDatasetProcessor(AssetProcessor):
    """
    Exports the files from a Dataset into Boonlib.
    """
    file_types = None

    def __init__(self):
        super(ExportDatasetProcessor, self).__init__()
        self.add_arg(Argument('dataset_id', 'str'))
        self.add_arg(Argument('boonlib_id', 'str'))

    def process(self, frame):
        asset = frame.asset
        boonlib_id = self.arg_value('boonlib_id')
        ds_id = self.arg_value('dataset_id')

        # Convert over labels.
        labels = asset.get_attr("labels")
        if not labels:
            self.logger.warning(f'The asset {asset.id} has no labels')
            return

        for label in labels:
            if label['datasetId'] == ds_id:
                new_label = label
                new_label['datasetId'] = "#__DSID__#"
                asset.set_attr("labels", [new_label])
                break

        # Convert thes files.
        copies = {}
        files = asset.get_files(category=['proxy', 'web-proxy'])
        for f in files:
            new_id = f'boonlib/{boonlib_id}/{asset.id}/{f.name}'
            copies[f.id] = new_id
            f._data['id'] = new_id
        self.copy_files_to_lib(copies)
        asset.set_attr("files", files)

        # Check label count
        if len(asset.get_attr("labels")) != 1:
            raise RuntimeError('Error converting labels')

        self.upload_asset(asset)

    def upload_asset(self, asset):
        # Save asset to bucket.
        jasset = to_json(asset, 2)
        size = str(len(jasset))
        libid = self.arg_value('boonlib_id')

        url = f'/api/v3/boonlib/_upload/{libid}/{asset.id}/asset.json'
        self.app.client.send_data(url, io.StringIO(jasset), size=size)

    def copy_files_to_lib(self, copy_map):
        req = {
            'paths': copy_map
        }
        self.app.client.post("/api/v3/boonlib/_copy_from_project", req)
