from django.contrib.auth.middleware import get_user
from django.utils.functional import SimpleLazyObject
from rest_framework.views import Request
from rest_framework_simplejwt.authentication import JWTAuthentication

class AuthenticationMiddlewareSimpleJWT(object):
    """
    Used to set the User on the request correctly, based upon the value coming back
    in the JWT claims. This does not happen by default when using JWT authentication
    and needs to be done manually.
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        request.user = SimpleLazyObject(lambda: self.__class__.get_jwt_user(request))
        return self.get_response(request)

    @staticmethod
    def get_jwt_user(request):
        user = get_user(request)
        if user.is_authenticated:
            return user

        try:
            user_jwt = JWTAuthentication().authenticate(Request(request))
            if user_jwt is not None:
                return user_jwt[0]
        except:
            return user
