# Generated by Django 3.0.8 on 2020-10-09 21:25

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('modules', '0002_data_migration_add_providers'),
    ]

    operations = [
        migrations.AlterField(
            model_name='provider',
            name='name',
            field=models.CharField(max_length=255, unique=True),
        ),
    ]
