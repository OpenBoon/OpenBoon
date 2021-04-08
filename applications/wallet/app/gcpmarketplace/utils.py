#!/usr/bin/env python3
import json

from django.conf import settings
from google.oauth2 import service_account
from googleapiclient.discovery import build


def get_google_credentials():
    return service_account.Credentials.from_service_account_info(
            json.loads(settings.MARKETPLACE_CREDENTIALS))


def get_procurement_api():
    return build('cloudcommerceprocurement', 'v1', cache_discovery=False,
                 static_discovery=False, credentials=get_google_credentials())


def get_service_control_api():
    return build('servicecontrol', 'v1', credentials=get_google_credentials())


def sum_ml_usage(project_usage):
    """Simple command that sums the output of Organization.get_ml_usage_for_time_period."""
    return {
        'tier_1_image_count': sum([u['tier_1_image_count'] for u in project_usage.values()]),
        'tier_1_video_hours': sum([u['tier_1_video_hours'] for u in project_usage.values()]),
        'tier_2_image_count': sum([u['tier_2_image_count'] for u in project_usage.values()]),
        'tier_2_video_hours': sum([u['tier_2_video_hours'] for u in project_usage.values()])
    }
