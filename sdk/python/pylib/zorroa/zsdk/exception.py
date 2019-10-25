class ZorroaSdkException(Exception):
    pass


class PluginException(ZorroaSdkException):
    pass


class ProcessorException(ZorroaSdkException):
    """
    The base class for processor exceptions.
    """
    pass


class UnrecoverableProcessorException(ProcessorException):
    """
    Thrown by a processor when it makes no sense to continue processing
    the asseet due to an unrecoverable error.
    """
    pass
