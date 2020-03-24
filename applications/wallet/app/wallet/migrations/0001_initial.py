from django.contrib.auth.models import User
from django.db import migrations


def forwards(apps, schema_editor):
    User.objects.create_superuser(username='admin@admin.com', email='admin@admin.com',
                                  password='admin', first_name='Admin', last_name='Adminson')


def reverse(apps, schema_editor):
    User.objects.get(username='admin').delete()


class Migration(migrations.Migration):

    dependencies = []

    operations = [migrations.RunPython(forwards, reverse)]
