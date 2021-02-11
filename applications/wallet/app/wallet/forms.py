import datetime
from django import forms

from projects.models import Project

YEAR_CHOICES = list(range(datetime.datetime.today().year, 2019, -1))


class CreateUsageReportForm(forms.Form):
    project = forms.CharField(required=False, widget=forms.Select)
    start_date = forms.DateField(required=False,
                                 widget=forms.SelectDateWidget(
                                     empty_label=('Year', 'Month', 'Day'),
                                     years=YEAR_CHOICES))
    end_date = forms.DateField(required=False,
                               widget=forms.SelectDateWidget(
                                   empty_label=('Year', 'Month', 'Day'),
                                   years=YEAR_CHOICES))

    def __init__(self, *args, **kwargs):
        super(CreateUsageReportForm, self).__init__(*args, **kwargs)
        choices = [(None, 'All Projects')]
        choices.extend([(p.id, p.name) for p in Project.objects.all()])
        self.fields['project'].widget.choices = choices

    def clean(self):
        start = self.cleaned_data.get('start_date')
        end = self.cleaned_data.get('end_date')
        if start and end:
            if start > end:
                self.add_error(None, 'Start date must come before End date.')
