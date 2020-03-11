# Generated by Django 2.2.11 on 2020-03-11 01:39

from django.db import migrations, models
import django.db.models.deletion
import multiselectfield.db.fields
import uuid


class Migration(migrations.Migration):

    initial = True

    dependencies = [
        ('projects', '0005_auto_20200225_0213'),
    ]

    operations = [
        migrations.CreateModel(
            name='Subscription',
            fields=[
                ('id', models.UUIDField(default=uuid.uuid4, primary_key=True, serialize=False)),
                ('video_hours_limit', models.IntegerField(default=0)),
                ('image_count_limit', models.IntegerField(default=0)),
                ('modules', multiselectfield.db.fields.MultiSelectField(blank=True, choices=[('zmlp-classification', 'Label Detection'), ('zmlp-objects', 'Object Detection'), ('zmlp-face-recognition', 'Facial Recognition'), ('zmlp-ocr', 'OCR (Optical Character Recognition)'), ('zmlp-deep-document', 'Page Analysis'), ('shot-detection', 'Shot Detection'), ('gcp-vision-crop-hints', 'Crop Hints (Vision)'), ('gcp-document-text-detection', 'OCR Documents (Vision)'), ('gcp-vision-text-detection', 'OCR Images (Vision)'), ('gcp-vision-label-detection', 'Label Detection (Vision)'), ('gcp-video-label-detection', 'Label Detection (Video)'), ('gcp-shot-detection', 'Shot Change (Video)'), ('gcp-explicit-content-detection', 'Explicit Content Detection (Video)')], max_length=276)),
                ('created_date', models.DateTimeField(auto_now_add=True)),
                ('modified_date', models.DateTimeField(auto_now=True)),
                ('project', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='projects.Project')),
            ],
        ),
    ]
