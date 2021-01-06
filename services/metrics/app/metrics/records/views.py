from datetime import datetime

from dateparser import parse as parse_date
from django.db.models import Sum, Q
from rest_framework import viewsets
from rest_framework.decorators import action, renderer_classes
from rest_framework.response import Response
from rest_framework.settings import api_settings

from metrics.records.models import ApiCall
from metrics.records.serializers import (ApiCallSerializer, ReportSerializer,
                                         UsageThisMonthSerializer)
from .renderers import ReportCSVRenderer
from .mixins import CSVFileMixin


class ApiCallViewSet(CSVFileMixin, viewsets.ModelViewSet):
    """CRUD Viewset for API Call records.

    Provides an additional method for aggregating calls by date
    """
    queryset = ApiCall.objects.all()
    serializer_class = ApiCallSerializer
    permission_classes = []
    renderer_classes(api_settings.DEFAULT_RENDERER_CLASSES + [ReportCSVRenderer])
    filename = 'billing_report.csv'

    def get_queryset(self):
        queryset = ApiCall.objects.all()
        project_id = self.request.query_params.get('project_id')
        asset_id = self.request.query_params.get('asset_id')
        service = self.request.query_params.get('service')

        if project_id is not None:
            queryset = queryset.filter(project=project_id)
        if asset_id is not None:
            queryset = queryset.filter(asset_id=asset_id)
        if service is not None:
            queryset = queryset.filter(service=service)

        queryset = queryset.order_by('created_date')
        return queryset

    def get_filename(self):
        filename = self.request.query_params.get('filename')
        if filename:
            return filename

        after = self.request.query_params.get('after')
        before = self.request.query_params.get('before')
        if after and before:
            return f'billing_report_{after}_to_{before}.csv'
        else:
            return self.filename

    @action(detail=False, methods=['get'],
            renderer_classes=api_settings.DEFAULT_RENDERER_CLASSES+[ReportCSVRenderer])
    def report(self, request):
        """Report on the usage per service by project during the specified time range.

        Filters all ApiCall records to the specified date range and then returns the usage
        on a per project & per service basis, allowing for billing by the api calls we
        would like to charge for.

        The `after` date is inclusive, and the `before` date is exclusive. This is meant
        to ensure apicalls that fall exactly on the start or end of a date range do not
        get double counted for billing.

        Setting the HTTP_ACCEPT header to csv on this request will return the data in
        CSV format.

        Args:
            request: The DRF request.

        Returns:
            (Response): JSON response containing the breakdown

        """
        queryset = self.get_queryset()

        # Filter to the date range specified, after inclusive, before exclusive
        after = self.request.query_params.get('after')
        before = self.request.query_params.get('before')
        if after:
            after_date = parse_date(after)
            queryset = queryset.filter(created_date__gte=after_date)
        if before:
            before_date = parse_date(before)
            queryset = queryset.filter(created_date__lt=before_date)

        # We want list of records where
        # Each row is project, list of distinct apis called, with sum of images and video

        # Get distinct service calls by project in this queryset
        # Order_by must match the distinct arguments in Postgres
        project_calls = queryset.order_by(
            'project', 'service'
        ).distinct(
            'project', 'service'
        ).values_list(
            'project', 'service'
        )

        # Find sums of image and video for each project/call pair
        data = []
        for project, service in project_calls:
            # Agg images from date filtered queryset by project and service call
            image_count = queryset.aggregate(
                image_count=Sum(
                    'image_count',
                    filter=Q(project=project, service=service)
                )
            )
            # Agg video minutes from date filtered queryset by project and service call
            video_minutes = queryset.aggregate(
                video_minutes=Sum(
                    'video_minutes',
                    filter=Q(project=project, service=service)
                )
            )
            data.append({
                'project': project,
                'service': service,
                'image_count': image_count['image_count'],
                'video_minutes': video_minutes['video_minutes']
            })

        serializer = ReportSerializer(data, many=True, context=self.get_serializer_context())
        return Response(serializer.data)

    @action(methods=['get'], detail=False)
    def usage_this_month(self, request):
        today = datetime.today()
        first_of_the_month = datetime(today.year, today.month, 1)
        project = request.query_params['project_id']
        records = self.get_queryset().filter(created_date__gte=first_of_the_month,
                                             project=project)
        totals = records.aggregate(image_count=Sum('image_count'),
                                   video_minutes=Sum('video_minutes'))
        totals['project'] = project
        serializer = UsageThisMonthSerializer(data=totals)
        serializer.is_valid(raise_exception=True)
        return Response(serializer.data)

