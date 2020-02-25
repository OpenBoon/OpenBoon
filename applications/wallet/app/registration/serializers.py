from django.conf import settings
from rest_auth.serializers import PasswordResetSerializer


class PasswordResetSerializer(PasswordResetSerializer):
    """Overrides the django rest auth serializer to send an html email."""

    def save(self):
        request = self.context.get('request')
        # Set some values to trigger the send_email method.
        opts = {
            'use_https': request.is_secure(),
            'from_email': getattr(settings, 'DEFAULT_FROM_EMAIL'),
            'request': request,
            'html_email_template_name': 'registration/password_reset_email.html',
            'extra_email_context': {'fqdn': settings.FQDN}
        }
        opts.update(self.get_email_options())
        self.reset_form.save(**opts)
