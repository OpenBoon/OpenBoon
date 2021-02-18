from datetime import timedelta

from axes.handlers.proxy import AxesProxyHandler
from django.conf import settings
from django.contrib.auth import get_user_model, logout, login, authenticate
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ObjectDoesNotExist, ValidationError
from django.core.mail import send_mail
from django.core.mail.message import sanitize_address
from django.db import transaction
from django.http import Http404
from django.template.loader import render_to_string
from django.utils.timezone import now
from djangorestframework_camel_case.parser import CamelCaseJSONParser
from google.auth.transport import requests
from google.oauth2 import id_token
from rest_auth.views import PasswordChangeView, PasswordResetView, \
    PasswordResetConfirmView
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from agreements.models import Agreement
from agreements.views import get_ip_from_request
from registration.models import UserRegistrationToken
from registration.serializers import RegistrationSerializer, UserSerializer
from wallet.mixins import CamelCaseRendererMixin

User = get_user_model()


class UserRegistrationView(APIView):
    """Allows anyone to sign up for a new account. The user is created and email
is sent with a link that will activate the account.

Example POST json body:

    {
        "email": "fake@gmail.com",
        "firstName": "Fakey",
        "lastName": "Fakerson",
        "password": "sjlhdffiuhdaifuh",
        "policiesDate": "200010101"
    }

Response Codes:

- 200 - User was registered and activation email was sent.
- 400 - Bad params in the request.
- 409 - Email address is already registered to an active user.
- 422 - The password given was not strong enough.

"""
    authentication_classes = []
    permission_classes = []

    def post(self, request, *args, **kwargs):
        serializer = RegistrationSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        validated_data = serializer.validated_data
        password = validated_data['password']
        email = validated_data['email']
        first = validated_data['firstName']
        last = validated_data['lastName']

        try:
            validate_password(password)
        except ValidationError as e:
            return Response({'detail': ['Password not strong enough.'],
                             'errors': e.messages},
                            status=status.HTTP_422_UNPROCESSABLE_ENTITY)

        with transaction.atomic():
            # If a registration token already exists for the user delete it so a new on
            # can be issued.
            if UserRegistrationToken.objects.filter(user__username=email).exists():
                token = UserRegistrationToken.objects.get(user__username=email)
                user = token.user
                user.set_password(password)
                user.save()
                token.delete()

            # If the user exists and there is no registration token then the user was already
            # activated. Exit with standard success message to prevent phishing.
            elif User.objects.filter(username=email).exists():
                return Response(data={'detail': ['Success, confirmation email has been sent.']})

            # If the user does not exist yet then create it.
            else:
                user = User.objects.create(username=email, email=email,
                                           first_name=first, last_name=last, is_active=False)
                user.set_password(password)
                user.save()

            # Issue a new registration token.
            token = UserRegistrationToken.objects.create(user=user)

            # Create an agreement.
            if validated_data.get('policiesDate'):
                Agreement.objects.create(user=user, policiesDate=validated_data['policiesDate'],
                                         ipAddress=get_ip_from_request(request))

        # Email the user a link to activate their account.
        subject = 'Welcome To ZVI - Please Activate Your Account.'
        html = render_to_string('registration/activation-email.html',
                                context={'fqdn': settings.FQDN,
                                         'token': token.token,
                                         'user_id': user.id})
        body = (f'Click this link to confirm your email address and activate your account.\n'
                f'{settings.FQDN}/accounts/confirm?'
                f'token={token.token}&userId={user.id}')
        try:
            sanitize_address(user.username, 'utf-8')
        except ValueError:
            user.delete()
            return Response(data={'detail': ['Email address invalid.']},
                            status=status.HTTP_422_UNPROCESSABLE_ENTITY)
        send_mail(subject=subject, message=body, html_message=html, fail_silently=False,
                  from_email='do_not_reply@boonai.io', recipient_list=[user.username])

        return Response(data={'detail': ['Success, confirmation email has been sent.']})


class UserConfirmationView(APIView):
    """Activates a newly created account. Requires a user id and registration token that
are sent in an email to the user on registration.

Example POST json body:

    {
        "userId": 7,
        "token": "20938092384-30948-9384-304984390"
    }

Response Codes:

- 200 - User was activated successfully.
- 400 - Bad params in request body.
- 404 - The token/userId is incorrect or the user was never actually registered.

"""
    authentication_classes = []
    permission_classes = []

    def post(self, request, *args, **kwargs):
        try:
            token = request.data['token']
            user_id = request.data['userId']
        except KeyError:
            msg = 'Confirming an email address requires sending the "token" and "userId" params.'
            return Response(data={'detail': [msg]}, status=status.HTTP_400_BAD_REQUEST)
        try:
            token = UserRegistrationToken.objects.get(token=token, user=user_id)
        except ObjectDoesNotExist:
            raise Http404('User ID and/or token does not exist.')
        if now() - token.createdAt > timedelta(days=settings.REGISTRATION_TIMEOUT_DAYS):
            msg = 'The activation link has expired. Please sign up again.'
            return Response(data={'detail': [msg]}, status=status.HTTP_403_FORBIDDEN)
        user = token.user
        user.is_active = True
        with transaction.atomic():
            user.save()
            token.delete()
        return Response(data={'detail': ['Success. User has been activated.']})


class ApiPasswordChangeView(PasswordChangeView):
    parser_classes = [CamelCaseJSONParser]


class ApiPasswordResetView(PasswordResetView):
    parser_classes = [CamelCaseJSONParser]


class ApiPasswordResetConfirmView(PasswordResetConfirmView):
    parser_classes = [CamelCaseJSONParser]


class MeView(CamelCaseRendererMixin, APIView):
    """Simple view that returns information about the current user."""
    parser_classes = [CamelCaseJSONParser]

    def get(self, request):
        return Response(UserSerializer(request.user, context={'request': request}).data)

    def patch(self, request):
        serializer = UserSerializer(request.user, data=request.data)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)


class LogoutView(APIView):
    """Basic logout view. Logs the user out and returns and empty json payload."""
    def post(self, request):
        logout(request)
        return Response({})


class LoginView(CamelCaseRendererMixin, APIView):
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
                login(request, user, backend='django.contrib.auth.backends.ModelBackend')
            except ValueError:
                return Response(data={'detail': ['Unauthorized: Bearer token invalid.']},
                                status=status.HTTP_401_UNAUTHORIZED)

        # Attempt to authenticate using username and password.
        else:
            username = request.data['username']
            password = request.data['password']
            user = authenticate(request, username=username, password=password)
            if user:
                login(request, user)
            else:
                credentials = {'username': username}
                if not AxesProxyHandler().is_allowed(request, credentials=credentials):
                    message = (
                        'This account has been locked due to too many failed login '
                        'attempts. Please contact support to unlock your account.')
                    return Response(data={'detail': [message]},
                                    status=status.HTTP_423_LOCKED)
                else:
                    message = 'Invalid email and password combination.'
                    return Response(data={'detail': [message]},
                                    status=status.HTTP_401_UNAUTHORIZED)
        return Response(UserSerializer(user, context={'request': request}).data)
