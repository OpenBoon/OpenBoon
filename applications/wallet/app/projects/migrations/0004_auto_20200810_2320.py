# Generated by Django 3.0.8 on 2020-08-10 23:20

from django.db import migrations, models
import django_cryptography.fields


class Migration(migrations.Migration):

    dependencies = [
        ('projects', '0003_recreate_membership_api_keys'),
    ]

    operations = [
        migrations.RenameField(
            model_name='project',
            old_name='is_active',
            new_name='isActive',
        ),
        migrations.AlterField(
            model_name='membership',
            name='apikey',
            field=django_cryptography.fields.encrypt(models.TextField(blank=True, editable=False)),
        ),
    ]