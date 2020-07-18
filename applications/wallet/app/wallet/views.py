from django.conf import settings
from django.contrib.auth import get_user_model, authenticate, login, logout
from django.contrib.auth.models import Group, User
from django.core.exceptions import ObjectDoesNotExist
from google.auth.transport import requests
from google.oauth2 import id_token
from rest_framework import viewsets, status
from rest_framework.response import Response
from rest_framework.routers import APIRootView
from rest_framework.views import APIView

from wallet.mixins import ConvertCamelToSnakeViewSetMixin
from wallet.serializers import UserSerializer


class UserViewSet(viewsets.ReadOnlyModelViewSet):
    """API endpoint that allows Users to be viewed or edited."""
    queryset = get_user_model().objects.all().order_by('-date_joined')
    serializer_class = UserSerializer

    def get_queryset(self):
        return User.objects.filter(id=self.request.user.id)


class LoginView(ConvertCamelToSnakeViewSetMixin, APIView):
    """Login view that supports Google OAuth bearer tokens passed in the "Authorization"
    header or a username and password sent in the JSON payload.

    """
    authentication_classes = []
    permission_classes = []

    def post(self, request, *args, **kwargs):

        # If the Authorization header is included attempt to authenticate using
        # Google OAuth.
        # The code below was taken from the Google OAuth docs -
        # https://developers.google.com/identity/sign-in/web/backend-auth
        if request.headers.get('Authorization'):
            token = request.headers.get('Authorization').split()[1]
            try:
                # Specify the CLIENT_ID of the app that accesses the backend:
                idinfo = id_token.verify_oauth2_token(token, requests.Request(),
                                                      settings.GOOGLE_OAUTH_CLIENT_ID)
                if idinfo['iss'] not in ['accounts.google.com',
                                         'https://accounts.google.com']:
                    raise ValueError('Wrong issuer.')
                email = idinfo['email']
                try:
                    user = User.objects.get(email=email)
                except ObjectDoesNotExist:
                    user = User.objects.create(username=email, email=email,
                                               first_name=idinfo.get('given_name'),
                                               last_name=idinfo.get('family_name'))
                login(request, user)
            except ValueError:
                return Response(data={'detail': 'Unauthorized: Bearer token invalid.'},
                                status=status.HTTP_401_UNAUTHORIZED)

        # Attempt to authenticate using username and password.
        else:
            user = authenticate(username=request.data['username'],
                                password=request.data['password'])
            if user:
                login(request, user)
            else:
                return Response(data={'detail': 'Unauthorized: Username & password invalid.'},
                                status=status.HTTP_401_UNAUTHORIZED)
        return Response(UserSerializer(user, context={'request': request}).data)


class LogoutView(ConvertCamelToSnakeViewSetMixin, APIView):
    """Basic logout view. Logs the user out and returns and empty json payload."""
    def post(self, request):
        logout(request)
        return Response({})


class MeView(ConvertCamelToSnakeViewSetMixin, APIView):
    """Simple view that returns information about the current user."""
    def get(self, request):
        return Response(UserSerializer(request.user, context={'request': request}).data)


class WalletAPIRootView(APIRootView):
    "Extends the default DRF API root view to allow adding extra views."
    def get(self, request, *args, **kwargs):
        from wallet.urls import BROWSABLE_API_URLS
        for view in BROWSABLE_API_URLS:
            self.api_root_dict[view[0]] = view[1].name
        return super(WalletAPIRootView, self).get(request, *args, **kwargs)
