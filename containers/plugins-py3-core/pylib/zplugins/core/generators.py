import os

from zorroa.zclient.exception import ArchivistException
from zorroa.zsdk import Generator, Argument, Asset, Document, Frame
from zorroa.zclient import get_zclient

class FileUploadGenerator(Generator):
    """
    A Generator for handling file uploads.

    Args:
        files: A list of dicts which describes uploaded files.
    """
    toolTips = {
        'files': 'A list of dicts which describes uploaded files.'
    }

    def __init__(self):
        super(FileUploadGenerator, self).__init__()
        self.add_arg(Argument('files', 'list', default=[], required=True,
                              toolTip=self.toolTips['files']))

    def generate(self, consumer):
        for files in self.arg_value('files'):
            asset = Asset(files['path'])
            asset.id = files['id']
            consumer.accept(Frame(asset))


class FileGenerator(Generator):
    """
    A Generator that emits a static list of files.  This generator can be used
    when you know the files you want to process.

    :param list paths: A list of files to emit for processing.
    """
    toolTips = {
        'paths': 'A list of files to emit for processing.'
    }

    def __init__(self):
        super(FileGenerator, self).__init__()
        self.add_arg(Argument('paths', 'list', default=[], required=True))

    def generate(self, consumer):
        for path in self.arg_value('paths'):
            consumer.accept(Frame(Asset(path)))


class FileSystemGenerator(Generator):
    """Recursively walks the given file paths and consume all files.

    Args:
        paths (list): A list of directories to walk.

    """
    toolTips = {
        'paths': 'A list of directories to walk.'
    }

    def __init__(self):
        super(FileSystemGenerator, self).__init__()
        self.add_arg(Argument('paths', 'list', default=[], required=True,
                              toolTip=self.toolTips['paths']))

    def generate(self, consumer):
        skip_prefixes = ('__', '.')
        for path in self.arg_value('paths'):
            for root, dirs, files in os.walk(path):
                dirs[:] = [dirname for dirname in dirs if not dirname.startswith(skip_prefixes)]
                for f in files:
                    if f.startswith(skip_prefixes):
                        continue
                    consumer.accept(Frame(Asset(os.path.join(root, f))))


class AssetSearchGenerator(Generator):
    """Generates frames based on an elastic search query.

    Args:
        search (str): Json formatted string representing a an elastic search query.
        max_assets (int): The maximum number of assets to process.
        page_size (int): The number of assets to fetch per page.
        scroll (str): The scroll timeout, ex: 5m.  Set to false for no scrolling.

    """
    toolTips = {
        'search': 'Json formatted string representing a an elastic search query.',
        'max_assets': 'The maximum number of items to iterate.',
        'page_size': 'The number of assets to fetch per request.',
        'scroll': 'The scan/scroll timeout value.  Set to false to disable ElasticSearch scrolling.'
    }

    def __init__(self):
        super(AssetSearchGenerator, self).__init__()
        self.add_arg(Argument('search', 'dict', required=True, toolTip=self.toolTips['search']))
        self.add_arg(Argument('max_assets', 'int', required=False,
                              default=0, toolTip=self.toolTips['max_assets']))
        self.add_arg(Argument('page_size', 'int', required=False,
                              default=32, toolTip=self.toolTips['page_size']))
        self.add_arg(Argument('scroll', 'string', required=False,
                              default="5m", toolTip=self.toolTips['scroll']))
        self.client = get_zclient()
        self.total_consumed = 0

    def generate(self, consumer):
        super(AssetSearchGenerator, self).generate(consumer)
        # Scrolling assets is the default operation. With scrolling it's
        # possible to iterate the entire database of assets without
        # sorting/paging overhead, but the scroll can timeout.
        # so it can't be used for exports currently.
        if self.arg_value("scroll"):
            self.scroll_assets(consumer)
        else:
            self.page_assets(consumer)

    def scroll_assets(self, consumer):
        search = self.arg_value('search')
        scroll = {"timeout": self.arg_value("scroll")}

        search['from'] = 0
        search['size'] = self.arg_value('page_size')
        search['scroll'] = scroll

        self.logger.info("Scrolling assets, page_size: {}, max_assets: {}".format(
            search['size'], self.arg_value('max_assets'))
        )

        try:
            while True:
                # Get the current search page.
                result = self.client.post('api/v3/assets/_search', search)

                # Consume any assets from this page of the search.
                documents = result.get('list', [])
                if not documents:
                    break

                # If consume_documents returns false, don't continue iteration
                if not self.consume_documents(consumer, documents):
                    return

                # Set the search to the scroll id to get the next page of the search.
                scroll = result.get('scroll')
                if not scroll:
                    raise ArchivistException('Invalid scrolling search, scroll Id was None.')
                search['scroll'] = scroll

        finally:
            scroll_id = search['scroll'].get('id')
            if scroll_id:
                try:
                    self.client.delete('api/v1/assets/_scroll', {'scroll_id': scroll_id})
                except Exception as e:
                    self.logger.warn('Failed to clear scroll: %s', e)

    def page_assets(self, consumer):
        search = self.arg_value('search')
        search['from'] = 0
        search['size'] = self.arg_value('page_size')

        self.logger.info("Iterating assets, page_size: {}, max_assets: {}".format(
            search['size'], self.arg_value('max_assets'))
        )

        while True:
            # Get the current search page.
            result = self.client.post('api/v3/assets/_search', search)

            # Consume any assets from this page of the search.
            documents = result.get('list', [])
            if not documents:
                break

            if not self.consume_documents(consumer, documents):
                break
            search['from'] += self.arg_value('page_size')

    def consume_documents(self, consumer, docs):
        max_assets = self.arg_value('max_assets')
        for document in docs:
            consumer.accept(Frame(Asset.from_document(Document(document))))
            self.total_consumed += 1
            if max_assets and self.total_consumed >= max_assets:
                return False
        return True


