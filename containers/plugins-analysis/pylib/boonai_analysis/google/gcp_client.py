

def initialize_gcp_client(client_class, transport=None):
    """
    Util function for creating Google service client.
    Args:
        client_class (class): The class we want to instance.
        transport: An optional grpc transport.

    Returns:
        Mixed: The proper service client.
    """
    if transport:
        return client_class(transport=transport)
    else:
        return client_class()
