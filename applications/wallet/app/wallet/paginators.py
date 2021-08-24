from collections import OrderedDict
from rest_framework.pagination import LimitOffsetPagination
from rest_framework.response import Response
from rest_framework.utils.urls import replace_query_param


class FromSizePagination(LimitOffsetPagination):
    limit_query_param = 'size'
    offset_query_param = 'from'


class ZMLPFromSizePagination(FromSizePagination):

    @property
    def asset_limit(self):
        if hasattr(self.request, 'max_assets'):
            return int(self.request.max_assets)
        else:
            return None

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
            self.display_page_controls = False

        if self.count == 0 or self.offset > self.count:
            return []

        return content

    def get_paginated_response(self, data):
        return Response(OrderedDict([
            ('count', self.count),
            ('next', self.get_next_link()),
            ('previous', self.get_previous_link()),
            ('results', self.get_limited_data(data))
        ]))

    def get_next_link(self):
        """Removes the next link if it would be returning results over the specified
        asset limit."""
        if self.asset_limit:
            if self.offset + self.limit >= self.asset_limit:
                return None
        return super(ZMLPFromSizePagination, self).get_next_link()

    def get_previous_link(self):
        """If someone requests a page "deep" into a request that is outside of the specified
        asset limit, crafts the appropriate previous link for the final page that would load under
        the asset limit."""
        if self.asset_limit:
            if self.offset - self.limit > self.asset_limit:
                # Determine correct offset for the "last" valid page
                whole_value = self.asset_limit // self.limit
                with_remainder = self.asset_limit / self.limit
                if whole_value == with_remainder:
                    # We can get a full final page
                    offset = self.limit * (whole_value - 1)
                else:
                    # Go one past to get the final partial page
                    offset = self.limit * (whole_value)
                url = self.request.build_absolute_uri()
                url = replace_query_param(url, self.limit_query_param, self.limit)
                return replace_query_param(url, self.offset_query_param, offset)

        return super(ZMLPFromSizePagination, self).get_previous_link()

    def get_limited_data(self, data):
        """Checks if there was a limit filter sent with this request, and if so truncates
        the amount of data returned appropriately."""
        if self.asset_limit:
            if self.offset > self.asset_limit:
                return []
            elif self.offset + self.limit <= self.asset_limit:
                return data
            else:
                to_include = self.asset_limit - self.offset
                return data[:to_include]
        else:
            return data


class NoPagination(FromSizePagination):
    """Sets an arbitrarily high page size to simulate no pagination."""
    default_limit = 10000
