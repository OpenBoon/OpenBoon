from rest_framework import status
from rest_framework.mixins import ListModelMixin
from rest_framework.response import Response
from rest_framework.settings import api_settings
from rest_framework.viewsets import GenericViewSet

from agreements.models import Agreement
from agreements.serializers import AgreementSerializer


def get_ip_from_request(request):
    """Returns the IP address from a request."""
    forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
    if forwarded_for:
        ip_address = forwarded_for.split(',')[0]
    else:
        ip_address = request.META.get('REMOTE_ADDR')
    return ip_address


class AgreementViewSet(ListModelMixin, GenericViewSet):
    """Viewset for working with Privacy & Terms Agreements"""
    serializer_class = AgreementSerializer

    def get_queryset(self):
        return Agreement.objects.filter(user=self.request.user)

    def create(self, request):
        user_pk = request.user.id

        # Check that we got a good policies date
        policies_date = request.data.get('policiesDate')
        if not policies_date:
            return Response(data={'detail': ['Missing `policiesDate` in the request.']},
                            status=status.HTTP_400_BAD_REQUEST)
        try:
            int(policies_date)
            if len(policies_date) != 8:
                raise ValueError
        except ValueError:
            return Response(data={'detail': ['Value for `policiesDate` must be an 8 character date '
                                  'string in the YYYYMMDD format.']},
                            status=status.HTTP_400_BAD_REQUEST)

        # Get the IP
        ip_address = get_ip_from_request(request)

        # Update request data with calculated values
        request.data['user'] = user_pk
        request.data['ipAddress'] = ip_address

        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        self.perform_create(serializer)
        headers = self.get_success_headers(serializer.data)
        return Response(serializer.data, status=status.HTTP_201_CREATED, headers=headers)

    def perform_create(self, serializer):
        serializer.save()

    def get_success_headers(self, data):
        try:
            return {'Location': str(data[api_settings.URL_FIELD_NAME])}
        except (TypeError, KeyError):
            return {}
