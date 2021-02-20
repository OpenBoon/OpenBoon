#!/usr/bin/env python3
import pprint

from boonsdk import app_from_env

app = app_from_env()

body = {
    'name': 'ProjectZERO',
    'id': '00000000-0000-0000-0000-000000000000'
}

pprint.pprint(app.client.post('/api/v1/projects', body))
