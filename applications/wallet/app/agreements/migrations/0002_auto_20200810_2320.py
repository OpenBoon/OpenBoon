# Generated by Django 3.0.8 on 2020-08-10 23:20

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('agreements', '0001_initial'),
    ]

    operations = [
        migrations.RenameField(
            model_name='agreement',
            old_name='created_date',
            new_name='createdDate',
        ),
        migrations.RenameField(
            model_name='agreement',
            old_name='ip_address',
            new_name='ipAddress',
        ),
        migrations.RenameField(
            model_name='agreement',
            old_name='modified_date',
            new_name='modifiedDate',
        ),
        migrations.RenameField(
            model_name='agreement',
            old_name='policies_date',
            new_name='policiesDate',
        ),
    ]
