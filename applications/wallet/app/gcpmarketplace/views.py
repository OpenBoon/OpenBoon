from django import forms
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.exceptions import PermissionDenied
from django.db import transaction
from django.shortcuts import render, redirect
from django.utils.decorators import method_decorator
from django.views import View
from django.views.decorators.csrf import csrf_exempt
from google.auth.transport import requests
from google.oauth2 import id_token

from gcpmarketplace.models import MarketplaceAccount
from gcpmarketplace.utils import get_procurement_api

User = get_user_model()


class SignUpForm(forms.Form):
    email = forms.CharField(label='Google Account Email Address', max_length=255)
    token = forms.CharField()


@method_decorator(csrf_exempt, name='dispatch')
class SignUpView(View):
    def post(self, request):
        form = SignUpForm(request.POST)

        # If the form is valid this means the new client has loaded the signup page
        # and filled out the form. We'll now activate the account.
        if form.is_valid():
            email = form.data['email']
            token = form.data['token']
            claims = self._get_jwt_claims(token)
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

            return redirect('gcpmarketplace-signup-success')

        # If the form is not valid we assume this is the initial post request from marketplace
        # and we need to render the signup page for the new client.
        else:
            context = {'token': request.POST['x-gcp-marketplace-token']}
            return render(request, 'gcpmarketplace/signup.html', context)

    def _get_jwt_claims(self, token):
        """Make sure the jwt was signed correctly and is not expired."""
        issuer = 'https://www.googleapis.com/robot/v1/metadata/x509/cloud-commerce-partner@system.gserviceaccount.com'  # noqa

        # TODO: Make audience configurable.
        idinfo = id_token.verify_token(token, requests.Request(), certs_url=issuer,
                                       audience='boonai.app')

        if idinfo['iss'] != issuer:
            raise PermissionDenied('Wrong issuer.')
        if not idinfo.get('sub'):
            raise PermissionDenied('JWT invalid. Missing "sub" claim.')
        return idinfo


def signup_success(request):
    return render(request, 'gcpmarketplace/success.html')
