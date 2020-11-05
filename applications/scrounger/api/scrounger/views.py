import json
from functools import wraps

import zmlp
from django.conf import settings
from django.contrib.auth import authenticate, logout, login
from django.contrib.auth.decorators import login_required
from django.core.exceptions import PermissionDenied
from django.http import JsonResponse, Http404, HttpResponse
from django.views.decorators.http import require_GET, require_POST

app = zmlp.ZmlpApp(apikey=settings.ZMLP_API_KEY, server=settings.ZMLP_API_URL)


def authentication_required(view_func):
    """View decorator that will return a 401 HTTP status and a blank body if the user is
    not authenticated.

    """
    @wraps(view_func)
    def wrapped_view(request, *args, **kwargs):
        if not request.user.is_authenticated:
            return JsonResponse({}, status=401)
        return view_func(request, *args, **kwargs)
    return wrapped_view


@require_POST
def login_view(request):
    """Basic log in view to authenticate a User.

    Http Methods: POST

    Body Params:
        username: Username of the person attempting to log in .
        password: Password of the person attempting to log in.

    Sample Response:
    {
        "firstName": "Faky",
        "lastName": "Fakerson",
        "username": "fakeperson",
        "email": "notreal@fake.com"
    }

    """
    if request.content_type == 'application/json':
        request_body = json.loads(request.body)
    else:
        request_body = request.POST
    username = request_body.get('username', '')
    password = request_body.get('password', '')
    user = authenticate(request, username=username, password=password)
    if user:
        login(request, user)
    else:
        message = 'No active user for the given email/password combination found.'
        return JsonResponse({'detail': message}, status=401)
    return JsonResponse({'firstName': user.first_name,
                         'lastName': user.last_name,
                         'username': user.username,
                         'email': user.email})


@require_POST
def logout_view(request):
    """View that logs out the current User.

    Http Methods: POST

    Sample Response:
    {}

    """
    logout(request)
    return JsonResponse({})


@require_GET
@authentication_required
def me_view(request):
    """View that returns information about the currently logged in User.

    Http Methods: GET

    Sample Response:
    {
        "firstName": "Faky",
        "lastName": "Fakerson",
        "username": "fakeperson",
        "email": "notreal@fake.com"
    }

    """
    data = {'firstName': request.user.first_name,
            'lastName': request.user.last_name,
            'username': request.user.username,
            'email': request.user.email}
    return JsonResponse(data)


@require_GET
@authentication_required
def search_view(request):
    """View that accepts a search string and returns matching assets. This view supports
    pagination, text searches and similarity searches. Text searches are simple plain text
    searches that return any asset that has metadata matching the search terms. Similarity
    searches take the ID of one or more assets and return assets that are visually similar.
    The text and similarity searches can be combined. Omitting search params will return
    all assets sorted by the newest first.

    Http Methods: GET

    Query Params:
        from: Index of the first search to start returning results for. To be used with
         the "size" query param for pagination.
        size: Number of search results to return. To be used with the "from" query param
         for pagination.
        text_search: String of search terms to return matching assets for.
        similarity_search: Comma-separated list of asset IDs to return similar assets for.

    Sample Response:
    {
        "assets": [
            {
                "id": "TyqDGfMqAOzpFVBdhzmJML4n7Ceekw9u",
                "name": "DogsAndCats.mp4",
                "path": "gs://my-pets-bucket/videos/DogsAndCats.mp4"
            }
        ]
    }

    """
    search = {}
    queries = []

    # Pagination
    if request.GET.get('from'):
        search['from'] = request.GET.get('from')
    if request.GET.get('size'):
        search['size'] = request.GET.get('size')

    # Text searches.
    if request.GET.get('text_search'):
        queries.append({
            'simple_query_string': {
                'query': request.GET.get('text_search')
            }
        })

    # Similarity searches.
    if request.GET.get('similarity_search'):
        simhashes = []
        for asset_id in request.GET.get('similarity_search').split(','):
            simhash = app.assets.get_asset(asset_id).get_attr('analysis.zvi-image-similarity.simhash')
            simhashes.append(simhash)
        sim_query = zmlp.SimilarityQuery(simhashes)
        queries.append(sim_query)

    # Put it all together and get the results.
    if queries:
        search['query'] = {
            'bool': {
                'must': queries
            }
        }
    assets = []
    for asset in app.assets.search(search=search):
        assets.append({'name': asset.get_attr('source.filename'),
                       'path': asset.get_attr('source.path'),
                       'type': asset.get_attr('mdedia.type'),
                       'id': asset.id})

    return JsonResponse({'assets': assets})


@require_GET
@authentication_required
def asset_thumbnail_proxy_view(request, asset_id):
    """View that returns a thumbnail image for the given asset ID.

    Path Args:
        asset_id(uuid): UUID of the asset to retrieve a thumbnail for.

    Http Methods: GET

    Sample Response:
    The response will be an image with mimetype "image/jpeg".

    """
    asset = app.assets.get_asset(asset_id)
    thumbnail = asset.get_thumbnail(0)
    _file = app.assets.download_file(thumbnail)
    return HttpResponse(_file.read(), content_type=thumbnail.mimetype)


@require_GET
@authentication_required
def asset_highres_proxy_view(request, asset_id):
    """View that returns the high resolution proxy of the given asset.

    Path Args:
        asset_id(uuid): UUID of the asset to retrieve a high res file for.

    Http Methods: GET

    Sample Response:
    The response will be an image with mimetype "image/jpeg" or video with mimetype "video/mp4".

    """
    asset = app.assets.get_asset(asset_id)
    videos = asset.get_files(mimetype="video/mp4", category="proxy")
    if videos:
        proxy = videos[0]
    else:
        try:
            proxy = asset.get_files(category='web-proxy')[0]
        except IndexError:
            return Http404(f'There is no web proxy available for asset {asset_id}')
    _file = app.assets.download_file(proxy)
    return HttpResponse(_file.read(), content_type=proxy.mimetype)
