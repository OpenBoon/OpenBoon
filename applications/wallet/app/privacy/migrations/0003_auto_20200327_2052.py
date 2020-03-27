# Generated by Django 2.2.11 on 2020-03-27 20:52

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('privacy', '0002_auto_20200327_1915'),
    ]

    operations = [
        migrations.AlterField(
            model_name='agreement',
            name='user',
            field=models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name='agreements', to=settings.AUTH_USER_MODEL),
        ),
        migrations.AlterUniqueTogether(
            name='agreement',
            unique_together=set(),
        ),
    ]
