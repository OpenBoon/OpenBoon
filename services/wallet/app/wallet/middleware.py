from django.contrib.auth.middleware import get_user
from django.utils.functional import SimpleLazyObject
from rest_framework.views import Request
from rest_framework_simplejwt.authentication import JWTAuthentication
from rest_framework_simplejwt.exceptions import InvalidToken


class AuthenticationMiddlewareSimpleJWT(object):
    """
    Used to set the User on the request correctly, based upon the value coming back
    in the JWT claims. This does not happen by default when using JWT authentication
    and needs to be done manually.
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        user = get_user(request)

        # If Authenticated with Session or BasicAuth, use that
        if user.is_authenticated:
            request.user = SimpleLazyObject(lambda: user)

        try:
            # If Authenticated with SimpleJWT, use that
            user_jwt = JWTAuthentication().authenticate(Request(request))
            if user_jwt is not None:
                request.user = SimpleLazyObject(lambda: user_jwt[0])
        except InvalidToken:
            # Otherwise, don't touch the request, as it messes with the Admin site
            pass

        return self.get_response(request)
