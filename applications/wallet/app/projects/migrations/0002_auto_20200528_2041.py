# Generated by Django 3.0.6 on 2020-05-28 20:41

from django.db import migrations, models
import projects.utils


class Migration(migrations.Migration):

    dependencies = [
        ('projects', '0001_initial'),
    ]

    operations = [
        migrations.AlterField(
            model_name='project',
            name='name',
            field=models.CharField(default=projects.utils.random_project_name, max_length=144),
        ),
    ]
