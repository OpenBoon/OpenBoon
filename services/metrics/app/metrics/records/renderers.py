from rest_framework_csv.renderers import CSVRenderer


class ReportCSVRenderer(CSVRenderer):
    header = ['project', 'service', 'tier', 'image_count', 'video_seconds']
