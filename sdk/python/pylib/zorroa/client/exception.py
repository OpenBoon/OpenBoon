__all__ = [
    "ArchivistException",
    "ArchivistSecurityException",
    "ArchivistConnectionException",
    "ArchivistWriteException",
    "DuplicateEntityException",
    "EntityNotFoundException",
    "InvalidRequestException"
]


class ArchivistException(Exception):
    """The base exception class for all Archivist client related Exceptions."""
    pass


class ArchivistRequestException(ArchivistException):
    """
    The base exception class for all exceptions thrown from the Archivist
    server.
    """

    def __init__(self, data):
        super(ArchivistException, self).__init__(
            data.get("message", "Unknown request exception"))
        self.__data = data

    @property
    def type(self):
        return self.__data["exception"]

    @property
    def cause(self):
        return self.__data["cause"]

    @property
    def endpoint(self):
        return self.__data["path"]

    @property
    def status(self):
        return self.__data["status"]

    def __str__(self):
        return "<ArchivistException msg=%s>" % self.__data["message"]


class ArchivistConnectionException(ArchivistException):
    """
    This exception is thrown if the client encounters a connectivity issue
    with the Archivist.
    """
    pass


class ArchivistWriteException(ArchivistRequestException):
    """
    This exception is thrown the Archivist fails a write operation.
    """

    def __init__(self, data):
        super(ArchivistWriteException, self).__init__(data)


class ArchivistSecurityException(ArchivistRequestException):
    """
    This exception is thrown if the Archivist fails a security check on the
    request.
    """

    def __init__(self, data):
        super(ArchivistSecurityException, self).__init__(data)


class EntityNotFoundException(ArchivistRequestException):
    """
    This exception is thrown if the Archivist fails a read operation because
    a piece of named data cannot be found.
    """

    def __init__(self, data):
        super(EntityNotFoundException, self).__init__(data)


class DuplicateEntityException(ArchivistWriteException):
    """
    This exception is thrown if the Archivist fails a write operation because
    the newly created element would be a duplicate.
    """

    def __init__(self, data):
        super(DuplicateEntityException, self).__init__(data)


class InvalidRequestException(ArchivistRequestException):
    """
    This exception is thrown if the request sent to Archivist is invalid in
    some way, similar to an IllegalArgumentException.
    """

    def __init__(self, data):
        super(InvalidRequestException, self).__init__(data)


"""
A map of HTTP response codes to local exception types.
"""
EXCEPTION_MAP = {
    404: EntityNotFoundException,
    409: DuplicateEntityException,
    400: InvalidRequestException,
    401: ArchivistSecurityException
}


def translate(status_code):
    """
    Translate the HTTP status code into one of the exceptions.

    Args:
        status_code (int): the HTTP status code

    Returns:
        Exception: the exception to throw for the given status code
    """
    return EXCEPTION_MAP.get(status_code, ArchivistRequestException)
