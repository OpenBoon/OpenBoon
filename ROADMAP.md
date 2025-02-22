# OpenBoon Roadmap
---

OpenBoon is the open source version of what was BoonAI. When the company went out of business the code was abandoned. A few years later, a couple of the original investors decided to open source the code.

As it was, BoonAI provided the following:

- An easy way to import images, documents and videos, selecting AI models to apply to those;
- A task manager to scale up jobs, both at import time and at various other times;
- An ElasticSearch back end where metadata is stored;
- Powerful search, powered by ElasticSearch and augmented with vector search, done at a time when Elastic didn't provide such feature;
- Both web and Python SDK front ends to interact with the assets;
- The ability to train and deploy simple classification and detection models on the assets

The plan is to make OpenBoin into a platform for preparing and curating datasets, focused on the needs of the animation and visual effects industry.

## Roadmap

### Bringing it back to life

- Update the various docker images, making sure they build with current versions of the base images, Python, etc
- Deploy pre-built images to the Github registry, to facilitate further development

## Moving it forward

These are in no particular order, issues should be created and prioritized/assigned/etc:

- Move to OpenSearch
- Delete GCS, Amazon, Clarifai models
- Replace Resnet embeddings with CLIP
- Add Natural Language search that uses CLIP
- Update Yolo object detection to latest version
- Promote the concept of "dataset"--currently there is the notion of datasets, used to fine tune simple models. These should be accessible through the main UI and basically be represented by an ES attribute, in order to allow users to prepare and curate real datasets to train diffusion and video models'
- Add a couple of image and video captioning models (BLIP, Llava, etc)
- Simplify the process of adding new models


The overall goal is to make OpenBoon into a visual dataset platform. The target user is versed in graphics, machine learning and computer vision. The platform is usable for general digital asset management, but the typical DAM user, who might be less technical, is not our user. The best way to work with DAM and OpenBoon is through an integration with some other DAM platform, but again, this is not the focus of the project for now.
