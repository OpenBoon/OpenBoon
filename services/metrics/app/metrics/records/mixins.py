from rest_framework.response import Response


class CSVFileMixin(object):
    """Allows for overriding the csv filename when downloaded."""

    filename = 'report.csv'

    def get_filename(self):
        return self.filename

    def finalize_response(self, request, response, *args, **kwargs):
        """Return response with content disposition setting the filename."""
        response = super(CSVFileMixin, self).finalize_response(
            request, response, *args, **kwargs
        )
        if isinstance(response, Response) and response.accepted_renderer.format == 'csv':
            response['content-disposition'] = f'attachment; filename={self.get_filename()}'

        return response
