import os
import csv
import requests
from django.conf import settings
from rest_framework.routers import APIRootView
from django.views.generic.edit import FormView
from django.http import HttpResponse

from wallet.forms import CreateUsageReportForm
from projects.models import Project


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

        # Build arguments to retrieve report values from Metric Service
        args = {}
        if data['project']:
            args['project'] = data['project']
        if data['start_date']:
            args['after'] = data['start_date']
        if data['end_date']:
            args['before'] = data['end_date']

        csv_response = requests.get(url, params=args)
        if csv_response.status_code != 200:
            return HttpResponse('Unable to reach Metrics service, please report to support.',
                                status_code=500)

        # Add project name, org, and org name, if available, to report data
        content = csv_response.json()
        all_project_ids = set([entry['project'] for entry in content])
        all_projects = Project.all_objects.filter(id__in=all_project_ids)
        projects_by_id = {str(project.id): project for project in all_projects}
        data = []
        for entry in content:
            project = projects_by_id.get(entry['project'])
            if project:
                entry['project_name'] = project.name
                if project.organization:
                    entry['organization_name'] = project.organization.name
                    entry['organization'] = project.organization.id
                else:
                    entry['organization_name'] = ''
                    entry['organization'] = ''
            data.append(entry)

        # Build a csv filename
        if len(all_projects) > 1:
            project_description = 'all_projects'
        else:
            project_description = all_projects[0].name.replace(' ', '_')
        if args.get('after'):
            start_description = f'_{args.get("after")}'
        else:
            start_description = ''
        if args.get('before'):
            end_description = f'_to_{args.get("before")}'
        else:
            end_description = ''
        filename = f'usage_report_{project_description}{start_description}{end_description}.csv'

        # Create the CSV response, Django-style since we're using Forms/FormView
        response = HttpResponse(content_type='text/csv')
        response['Content-Disposition'] = f'attachment; filename="{filename}"'

        writer = csv.writer(response)
        row_headers = ['Project ID',
                       'Project Name',
                       'Organization ID',
                       'Organization Name',
                       'Tier',
                       'Service',
                       'Image Count',
                       'Video Minutes']
        writer.writerow(row_headers)
        current_project = None
        for row in data:
            # Insert row break if project is different
            if current_project is not None and current_project != row['project']:
                writer.writerow([''] * len(row_headers))
            writer.writerow([row['project'],
                             row['project_name'],
                             row['organization'],
                             row['organization_name'],
                             row['tier'],
                             row['service'],
                             row['image_count'],
                             row['video_minutes']])
            current_project = row['project']

        return response
