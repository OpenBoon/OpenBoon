# Generated by Django 2.2.11 on 2020-03-31 01:31

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('agreements', '0001_initial'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='agreement',
            name='privacy_policy_filename',
        ),
        migrations.RemoveField(
            model_name='agreement',
            name='terms_and_conditions_filename',
        ),
        migrations.AddField(
            model_name='agreement',
            name='policies_date',
            field=models.CharField(default='00000000', max_length=8),
        ),
    ]
