# Generated by Django 3.2.5 on 2021-08-11 17:42

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('records', '0007_alter_apicall_unique_together'),
    ]

    operations = [
        migrations.AlterField(
            model_name='apicall',
            name='asset_path',
            field=models.TextField(blank=True, default=''),
        ),
        migrations.AlterField(
            model_name='apicall',
            name='service',
            field=models.TextField(),
        ),
    ]
