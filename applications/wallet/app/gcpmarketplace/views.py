from django import forms
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.exceptions import PermissionDenied
from django.db import transaction
from django.http import HttpResponseRedirect
from django.shortcuts import render
from django.urls import reverse
from django.utils.decorators import method_decorator
from django.views import View
from google.auth.transport import requests
from google.oauth2 import id_token

from gcpmarketplace.models import MarketplaceAccount
from gcpmarketplace.utils import get_procurement_api

User = get_user_model()


class SignUpForm(forms.Form):
    email = forms.CharField(label='Google Account Email Address', max_length=255)


class SignUpView(View):
    def get(self, request):
        form = SignUpForm()
        return render(request, 'gcpmarketplace/signup.html', {'form': form})

    def post(self, request):
        form = SignUpForm(request.POST)
        if form.is_valid():
            email = form.data['email']
            claims = self._get_jwt_claims(request)
            account_name = f'providers/{settings.MARKETPLACE_PROJECT_ID}/accounts/{claims["sub"]}'

            # Create the User and MarketplaceAccount in Wallet
            with transaction.atomic():
                user, created = User.objects.get_or_create(email=email, username=email)
                if created:
                    user.is_active = False
                    user.save()
                MarketplaceAccount.objects.get_or_create(name=account_name, user=user)

            # Send approval to marketplace.
            request = get_procurement_api().providers().accounts().approve(
                name=account_name, body={'approvalName': 'signup'})
            request.execute()

            return HttpResponseRedirect(reverse('gcpmarketplace-signup-success'))

    def _get_jwt_claims(self, request):
        """Make sure the jwt was signed correctly and is not expired."""
        issuer = 'https://www.googleapis.com/robot/v1/metadata/x509/cloud-commerce-partner@system.gserviceaccount.com'  # noqa
        token = request.headers.get('x-gcp-marketplace-token').split()[1]
        idinfo = id_token.verify_token(token, requests.Request(), certs_url=issuer,
                                       audience='zvi.zorroa.com')  # TODO: Make audience configurable..
        if idinfo['iss'] != issuer:
            raise PermissionDenied('Wrong issuer.')
        if not idinfo.get('sub'):
            raise PermissionDenied('JWT invalid. Missing "sub" claim.')
        return idinfo


def signup_success(request):
    return render(request, 'gcpmarketplace/success.html')
