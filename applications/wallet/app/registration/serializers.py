from django.conf import settings
from django.contrib.auth.models import User
from rest_auth.serializers import PasswordResetSerializer
from rest_framework import serializers

from projects.models import Membership, Project


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


class RegistrationSerializer(serializers.Serializer):
    email = serializers.CharField(required=True)
    firstName = serializers.CharField(required=True)
    lastName = serializers.CharField(required=True)
    password = serializers.CharField(required=True)
    policiesDate = serializers.CharField(required=False)


class UserSerializer(serializers.ModelSerializer):
    roles = serializers.SerializerMethodField()
    agreed_to_policies_date = serializers.SerializerMethodField()

    class Meta:
        model = User
        depth = 1
        fields = ['id', 'username', 'first_name', 'last_name', 'email', 'last_login',
                  'date_joined', 'roles', 'agreed_to_policies_date']
        read_only_fields = ['id', 'username', 'email', 'last_login', 'date_joined',
                            'roles', 'agreed_to_policies_date']

    def get_roles(self, obj):
        # Adds roles for all projects the user is a member of.
        memberships = Membership.objects.filter(user=obj)
        roles = {}
        for membership in memberships:
            roles[str(membership.project.id)] = membership.roles
        owned_projects = Project.objects.filter(organization__owners=obj)
        for project in owned_projects:
            roles[str(project.id)] = [r['name'] for r in settings.ROLES]
        return roles

    def get_agreed_to_policies_date(self, obj):
        agreements = obj.agreements.order_by('-createdDate')
        if len(agreements) == 0:
            return '00000000'
        return agreements[0].policiesDate
