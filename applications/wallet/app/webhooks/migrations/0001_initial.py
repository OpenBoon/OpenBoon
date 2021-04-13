# Generated by Django 3.1.7 on 2021-04-08 02:10

from django.db import migrations, models


def add_triggers(apps, schema_editor):
    Trigger = apps.get_model('webhooks', 'Trigger')
    Trigger.objects.bulk_create([
        Trigger(name='ASSET_ANALYZED', displayName='Asset Analyzed',
                description='Asset is added to the Boon AI with initial analysis.'),
        Trigger(name='ASSET_MODIFIED', displayName='Asset Modified',
                description='Asset is modified through additional analysis or manual editing.')
    ], ignore_conflicts=True)


class Migration(migrations.Migration):

    initial = True

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='Trigger',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('name', models.CharField(max_length=64, unique=True)),
                ('displayName', models.CharField(max_length=64, unique=True)),
                ('description', models.CharField(max_length=255)),
            ],
        ),
        migrations.RunPython(add_triggers)
    ]
