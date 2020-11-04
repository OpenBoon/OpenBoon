import json
from django.http import JsonResponse
from django.views import View
from django.contrib.auth import get_user_model, logout, login, authenticate


User = get_user_model()


class LoginView(View):
    """Basic Login view. Supports basic username/password login from the post body."""

    def post(self, request):
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


class LogoutView(View):
    """Basic Logout view. Logs the user out and returns an empty JSON response."""

    def post(self, request):
        logout(request)
        return JsonResponse({})
