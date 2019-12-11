import logging
import os

from django.conf import settings
from django.contrib.auth import get_user_model, authenticate, login, logout
from django.contrib.auth.models import Group
from django.http import HttpResponse, JsonResponse
from django.views.generic import View
from rest_framework import viewsets
from rest_framework.views import APIView

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


class LoginView(APIView):
    """Basic login view that authenticates the user and returns the user's info."""
    authentication_classes = []
    permission_classes = []


    def post(self, request, *args, **kwargs):
        user = authenticate(username=request.data['username'],
                            password=request.data['password'])
        if user:
            login(request, user)
            return JsonResponse({'id': user.id,
                                 'username': user.username,
                                 'email': user.email,
                                 'first_name': user.first_name,
                                 'last_name': user.last_name})
        else:
            return HttpResponse('Unauthorized', status=401)


class LogoutView(APIView):
    """Basic logout view. Logs the user out and returns and empty json payload."""
    def post(self, request):
        logout(request)
        return JsonResponse({})
