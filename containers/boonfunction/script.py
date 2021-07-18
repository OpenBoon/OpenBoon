
def process(asset):

    # Make a response
    rsp = {
        'analysis': [
            {
                "type": "labels",
                "predictions": [
                    {
                        "label": "cat",
                        "score": 0.99
                    }
                ]
            }
        ],
        'custom': {
            'some_field1': "some_value",
            'some_field2': "some_value",
        }
    }

    return rsp
