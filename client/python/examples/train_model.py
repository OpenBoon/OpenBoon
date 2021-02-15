#!/usr/bin/env python3
from boonsdk import app_from_env
from boonsdk.entity import ModelType

import urllib3
urllib3.disable_warnings()

app = app_from_env()

model_name = "CHANGE THIS TO MODEL NAME"
model_type = ModelType.ZVI_KNN_CLASSIFIER  # CHANGE THIS MODEL TYPE

if __name__ == '__main__':
    # reference of model creation
    # model = app.models.create_model(name=model_name, type=model_type)
    model = app.models.find_one_model(name=model_name)
    app.models.train_model(model=model, deploy=True)
