from rest_framework_csv.renderers import CSVRenderer


class ReportCSVRenderer(CSVRenderer):
    header = ['project', 'service', 'image_count', 'video_minutes']
