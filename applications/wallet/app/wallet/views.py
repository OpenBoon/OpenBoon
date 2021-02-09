import os
import requests
from django.conf import settings
from rest_framework.routers import APIRootView
from django.views.generic.edit import FormView
from django.http import HttpResponse

from wallet.forms import CreateUsageReportForm


class WalletAPIRootView(APIRootView):
    "Extends the default DRF API root view to allow adding extra views."
    def get(self, request, *args, **kwargs):
        from wallet.urls import BROWSABLE_API_URLS
        for view in BROWSABLE_API_URLS:
            self.api_root_dict[view[0]] = view[1].name
        return super(WalletAPIRootView, self).get(request, *args, **kwargs)


class UsageReportView(FormView):
    template_name = 'admin/generate_usage_report.html'
    form_class = CreateUsageReportForm
    success_url = '/admin/'

    def form_valid(self, form):
        url = os.path.join(settings.METRICS_API_URL, 'api/v1/apicalls/report')
        data = form.cleaned_data
        args = {}
        if data['project']:
            args['project'] = data['project']
        if data['start_date']:
            args['after'] = data['start_date']
        if data['end_date']:
            args['before'] = data['end_date']

        args['format'] = 'csv'
        csv_response = requests.get(url, params=args)
        if csv_response.status_code == 200:
            response = HttpResponse(csv_response.content,
                                    content_type=csv_response.headers['Content-Type'])
            response['Content-Disposition'] = csv_response.headers['Content-Disposition']
            return response
        else:
            return HttpResponse('Unable to reach Metrics service, please report to support.',
                                status_code=500)
