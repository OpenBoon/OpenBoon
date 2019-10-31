import os
import logging

from django.contrib.auth.decorators import login_required
from django.views.generic import View
from django.http import HttpResponse
from django.conf import settings


class FrontendAppView(View):
    """
    Serves the compiled frontend application entry point. If this is raising an error,
    be sure to run `npm run build`.
    """

    def get(self, request):
        try:
            with open(os.path.join(settings.REACT_APP_DIR, 'build', 'index.html')) as _file:
                return HttpResponse(_file.read())
        except FileNotFoundError as e:
            logging.exception('Could not find a Production build of the frontend. '
                              'Try running `npm run build` and retrying.')
            msg = ("""
                A production build of the frontend was not found. You need to run 
                `npm run build` in order to build the frontend so it can be served.
            """)
            return HttpResponse(msg, status=501)
