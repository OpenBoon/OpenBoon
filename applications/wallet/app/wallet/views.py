import os
import logging

from django.contrib.auth import get_user_model, authenticate, login
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_POST
from django.views.generic import View
from django.http import HttpResponse, JsonResponse
from django.conf import settings
from django.contrib.auth.models import Group
from rest_framework import viewsets
from wallet.serializers import UserSerializer, GroupSerializer


class FrontendAppView(View):
    """
    Serves the compiled frontend application entry point. If this is raising an error,
    be sure to run `npm run build`.
    """

    def get(self, request):
        try:
            with open(os.path.join(settings.REACT_APP_DIR, 'build', 'index.html')) as _file:
                return HttpResponse(_file.read())
        except FileNotFoundError:
            logging.exception('Could not find a Production build of the frontend. '
                              'Try running `npm run build` and retrying.')
            msg = ("""
                A production build of the frontend was not found. You need to run
                `npm run build` in order to build the frontend so it can be served.
            """)
            return HttpResponse(msg, status=501)


class UserViewSet(viewsets.ModelViewSet):
    """API endpoint that allows Users to be viewed or edited."""
    queryset = get_user_model().objects.all().order_by('-date_joined')
    serializer_class = UserSerializer


class GroupViewSet(viewsets.ModelViewSet):
    """API endpoint that allows Groups to be viewed or edited."""
    queryset = Group.objects.all()
    serializer_class = GroupSerializer


@csrf_exempt
@require_POST
def login_view(request):
    """Basic login view that authenticates the user and returns the user's info."""
    print(request.POST['username'], request.POST['password'])
    user = authenticate(username=request.POST['username'], password=request.POST['password'])
    if user:
        login(request, user)
        return JsonResponse({'id': user.id,
                             'username': user.username,
                             'email': user.email,
                             'first_name': user.first_name,
                             'last_name': user.last_name})
    else:
        return HttpResponse('Unauthorized', status=401)
