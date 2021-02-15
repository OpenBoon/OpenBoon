#!/usr/bin/env python3
from boonsdk import app_from_env

app = app_from_env()


def delete(job_id='', batch_max=10):
    """ Delete an asset from a query based on Job ID

    Args:
        job_id: (str) the Job ID which added the asset
        batch_max: (int) max batch value, default 10

    Returns:
        None
    """
    q = {
        "size": batch_max,
        "query": {
            "match": {
                "system.jobId": {
                    "query": job_id
                },
            }
        }
    }

    search = app.assets.search(q)
    for asset in search:
        print("deleting {}".format(asset.document['source']['filename']))
        app.assets.delete_asset(asset)


def add(model_name="", names=[], batch_max=10):
    """ Batch add a label to a query of assets

    Args:
        model_name: (str) Model.name value
        names: (List[str]) list of strings to iterate through for batch labeling
        batch_max: (int) max batch value, default 10

    Returns:
        None
    """
    model = app.models.find_one_model(name=model_name)

    for name in names:
        q = {
            "size": batch_max,
            "query": {
                "match": {
                    "source.filename.fulltext": {
                        "query": name
                    },
                }
            }
        }

        label = model.make_label(name)

        search = app.assets.search(q).batches_of(batch_max)
        for batch in search:
            print("labeling {} as {}".format(len(batch), label.label))
            app.assets.update_labels(batch, label)


if __name__ == '__main__':
    # Uncomment add or delete and set args
    # Uncomment to add labels to batches of assets
    add(
        model_name="model_name",  # CHANGE THIS
        names=['rain', 'overcast', 'sunny', 'snow'],  # CHANGE THIS
        batch_max=100,  # CHANGE THIS
    )
    #
    # Uncomment to delete a single asset based on system.jobId that asset was imported in
    # delete(
    #     job_id="e5431800-d6b7-11ea-8cb3-22473eba6bf5",  # CHANGE THIS
    #     batch_max=100,  # CHANGE THIS
    # )
