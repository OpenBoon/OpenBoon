# Generated by Django 3.1.5 on 2021-03-17 16:24

from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ('organizations', '0002_auto_20210128_0017'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='organization',
            name='owner',
        ),
        migrations.AddField(
            model_name='organization',
            name='owners',
            field=models.ManyToManyField(to=settings.AUTH_USER_MODEL),
        ),
    ]