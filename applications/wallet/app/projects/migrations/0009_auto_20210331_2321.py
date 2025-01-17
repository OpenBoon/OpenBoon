# Generated by Django 3.1.7 on 2021-03-31 23:21

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('organizations', '0004_auto_20210317_2308'),
        ('projects', '0008_project_apikey'),
    ]

    operations = [
        migrations.AlterField(
            model_name='project',
            name='name',
            field=models.CharField(max_length=144),
        ),
        migrations.AlterUniqueTogether(
            name='project',
            unique_together={('name', 'organization')},
        ),
    ]
