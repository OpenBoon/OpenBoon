

def local_dev_cors_middleware(get_response):
    """
    Adds CORS headers for local development since the front and backend servers are
    being served from different locations.
    """

    def middleware(request):
        response = get_response(request)
        response['Access-Control-Allow-Origin'] = 'http://localhost:3000'
        response['Access-Control-Allow-Methods'] = 'GET, POST, PUT, PATCH, OPTIONS, DELETE, HEAD'
        response['Access-Control-Allow-Headers'] = 'Content-Type, X-CSRFToken'
        response['Access-Control-Allow-Credentials'] = 'true'
        return response

    return middleware