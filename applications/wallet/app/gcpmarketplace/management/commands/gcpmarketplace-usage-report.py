import datetime
import pprint
import uuid
from time import sleep

from django.conf import settings
from django.core.management import BaseCommand

from gcpmarketplace.models import MarketplaceEntitlement
from gcpmarketplace.utils import get_service_control_api, get_procurement_api, sum_ml_usage


class UsageReporter():
    # Handles reporting project usage to Google Marketplace.

    def report(self):
        """Loops over all active entitlements and sends usage information for each of them."""
        entitlements = self._get_active_entitlements()
        if not entitlements:
            print('No active entitlements. No usage reports to send.')
            return
        for entitlement in entitlements:
            self._report_usage(entitlement)

    def _get_active_entitlements(self):
        """Returns a list of all active marketplace entitlements."""
        request = get_procurement_api().providers().entitlements().list(
            parent=f'providers/{settings.MARKETPLACE_PROJECT_ID}',
            filter='state=active')
        return request.execute().get('entitlements')

    def _get_usage(self, entitlement, start_time, end_time):
        """Gives usage info by tiers on all projects associated with this entitlement."""
        entitlement = MarketplaceEntitlement.objects.get(name=entitlement['name'])
        project_usage = entitlement.organization.get_ml_usage_for_time_period(start_time, end_time)
        return sum_ml_usage(project_usage)

    def _report_usage(self, entitlement):
        """Sends usage information to marketplace for the given entitlement."""
        ServiceControlApi = get_service_control_api()
        time_format = '%Y-%m-%dT%H:%M:%SZ'
        end_time = datetime.datetime.utcnow()
        start_time = end_time - datetime.timedelta(hours=1)
        usage = self._get_usage(entitlement, start_time, end_time)
        if not usage:
            print(f'No usage data. Skipping report for entitlement {entitlement["name"]}.')
            return
        operation = {
            'operationId': str(uuid.uuid4()),
            'operationName': 'ZVI Usage Report',
            'consumerId': entitlement['usageReportingId'],
            'startTime': start_time.strftime(time_format),
            'endTime': end_time.strftime(time_format),
            'metricValueSets': [
                {
                    'metricName': f'{settings.MARKETPLACE_SERVICE_NAME}/{entitlement["plan"]}_video_non_named_processed',
                    'metricValues': [{'int64Value': int(usage['tier_1_video_hours'])}]},
                {
                    'metricName': f'{settings.MARKETPLACE_SERVICE_NAME}/{entitlement["plan"]}_image_non_named',
                    'metricValues': [{'int64Value': int(usage['tier_1_image_count'])}]},
                {
                    'metricName': f'{settings.MARKETPLACE_SERVICE_NAME}/{entitlement["plan"]}_video_named_processed',
                    'metricValues': [{'int64Value': int(usage['tier_2_video_hours'])}]},
                {
                    'metricName': f'{settings.MARKETPLACE_SERVICE_NAME}/{entitlement["plan"]}_image_named',
                    'metricValues': [{'int64Value': int(usage['tier_2_image_count'])}]}
            ]
        }
        check = ServiceControlApi.services().check(
            serviceName=settings.MARKETPLACE_SERVICE_NAME, body={
                'operation': operation
            }).execute()
        if 'checkErrors' in check:
            print('Errors for user %s with product %s:' % (entitlement['account'],
                                                           entitlement['product']))
            print(check['checkErrors'])
            return
        print(f'Sending report:\n{pprint.pformat(operation)}')
        ServiceControlApi.services().report(
            serviceName=settings.MARKETPLACE_SERVICE_NAME, body={
                'operations': [operation]
            }).execute()


class Command(BaseCommand):
    help = 'Starts service that sends usage reports to gcp marketplace every hour.'

    def handle(self, *args, **options):
        usage_reporter = UsageReporter()
        try:
            while True:
                print('Sending usage report to marketplace.')
                usage_reporter.report()
                print('Usage reporting successful. Sleeping for 1 hour.')
                sleep(60 * 60)
        except KeyboardInterrupt:
            print('Program terminated by user. Goodbye.')
            return
