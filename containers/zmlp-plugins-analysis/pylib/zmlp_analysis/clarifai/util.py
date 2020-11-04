import os

from clarifai.rest import ClarifaiApp
from clarifai_grpc.channel.clarifai_channel import ClarifaiChannel
from clarifai_grpc.grpc.api import service_pb2_grpc


def get_clarifai_app():
    """
    Return a usable ClarifaiApp

    Returns:
        ClarifaiApp: The configured ClarifaiApp
    """
    return ClarifaiApp(api_key=os.environ.get('CLARIFAI_KEY'))


def get_clarifai_grpc_app():
    """
    Return a usable Clarifai gRPC

    Returns:
        V2Stub: Configured Clarifai gRPC Client
    """
    return service_pb2_grpc.V2Stub(ClarifaiChannel.get_json_channel())
