#!/usr/bin/env python3
import pprint

from boonsdk import app_from_env

query = {
    "size": 100,
    "query": {
        "match_all": {}
    }
}

app = app_from_env()


def delete_all_assets(search):
    batch = []
    for a in app.assets.scroll_search(search):
        batch.append(a.id)
        if len(batch) >= 50:
            pprint.pprint(app.assets.batch_delete_assets(batch))
            batch = []

    # Handle left over batch
    if batch:
        pprint.pprint(app.assets.batch_delete_assets(batch))


if __name__ == '__main__':
    delete_all_assets(query)
