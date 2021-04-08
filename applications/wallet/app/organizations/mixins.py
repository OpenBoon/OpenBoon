import re

from rest_framework import filters
from rest_framework.filters import OrderingFilter
from rest_framework.viewsets import GenericViewSet


class CamelCaseOrderingFilter(OrderingFilter):
    """Extends the ordering filter to convert all camelcase entries to snake case. Used
    for ordering Users that have snake_case fields but get camelCase fields from the
    frontend."""
    def get_ordering(self, request, queryset, view):
        ordering = super(CamelCaseOrderingFilter, self).get_ordering(request, queryset, view)
        if ordering:
            regex = re.compile(r'(?<!^)(?=[A-Z])')
            ordering = [regex.sub('_', f).lower() for f in ordering]
        return ordering


class SortAndSearchUsersMixin(GenericViewSet):
    """Adds sort and search capabilities to viewset that return Users."""
    filter_backends = [filters.SearchFilter, CamelCaseOrderingFilter]
    search_fields = ['first_name', 'last_name', 'email']
    ordering_fields = ['firstName', 'last_name', 'email']
