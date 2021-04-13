from rest_framework.pagination import LimitOffsetPagination


class FromSizePagination(LimitOffsetPagination):
    limit_query_param = 'size'
    offset_query_param = 'from'


class ZMLPFromSizePagination(FromSizePagination):

    def prep_pagination_for_api_response(self, content, request):
        """
        Allows you to use this Paginator to prep responses returned from the ZMLP API.

        The normal method to prep a response would follow this pattern:

        ```
        paginator.prep_pagination_for_api_response(content, request)
        return paginator.get_paginated_response(content['list'])
        ```

        *Note*: We often only want to return the 'list' key in the content body, as that
        contains the list of resources and will be placed under the `results` key in
        our api response.

        Args:
            content: The JSON content body from the ZMLP API
            request: The original request

        Returns:
            The content body.
        """
        self.request = request
        try:
            self.count = content['page']['totalCount']
        except KeyError:
            self.count = None

        self.limit = self.get_limit(request)
        if self.limit is None:
            return None

        self.offset = self.get_offset(request)
        if self.count > self.limit and self.template is not None:
            self.display_page_controls = True

        if self.count == 0 or self.offset > self.count:
            return []

        return content


class NoPagination(FromSizePagination):
    """Sets an arbitrarily high page size to simulate no pagination."""
    default_limit = 10000
