# flake8: noqa
from boonflow import AssetProcessor, Argument


class MyExampleProcessor(AssetProcessor):

    """
    The file extensions this processor handles.  Assets that do not match
    the supplied file_types property will not be run through the processor.
    Defaults to all supported file types.
    """
    file_types = frozenset(["jpg"])

    """
    If true, the processing system will run multiple Assets through the Processor
    instance in parallel.  If your process() function is not thread safe, either
    add locking or set this to false.
    """
    use_threads = True

    def __init__(self):
        self.add_arg(Argument('example_arg', 'str', default="hello", required=True))

    def init(self):
        """
        The init() function is run once for each batch of files.  Use this
        method to initialize class members that would be expensive to create
        on per-asset basis. For example, if you are using Tensorflow, load
        your model here.
        """
        pass

    def process(self, frame):
        """
        The process() function is run once per Asset in the batch.  Use this
        method to make modifications to the Asset directly.  The attributes
        set on the Asset must map to an existing Field on the server.

        Args:
            frame (Frame): A data frame containing an Asset instance to be
                processed.
        """
        asset = frame.asset

        # To skip an asset, set frame.skip = True.  Skipping an asset
        # will cease all processing and remove the asset from the database.
        if asset.get_attr("source.filesize") < 1024:
            frame.skip = True
            return

        # To get an argument value, use the arg_value method.
        val = self.arg_value("example_arg")

        # To set an individual field value, use the set_attr method on the asset.
        frame.asset.set_attr("custom.example_string", "cat")

    def teardown(self):
        """
        The teardown() function is run once at the end of the batch.  Use this method
        to clean up or finalize any resources that were created in the init() function.
        """
        pass
