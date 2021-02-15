#!/usr/bin/env python3
import zmq
import json

payload = {
    "type": "execute",
    "payload": {
        "ref": {
            "className": "boonai_core.image.importers.ImageImporter",
            "image": "boonai/plugins-core",
            "args": {
                "extract_extended_metadata": True
            }
        },
        "asset": {
            "id": "12345",
            "document": {
                "source": {
                    "path": "gs://boonai-dev-data/image/pluto.png",
                    "mediatype": "image/jpeg"
                }
            }
        }
    }
}

ctx = zmq.Context()
socket = ctx.socket(zmq.PAIR)
socket.connect("tcp://localhost:5001")
socket.send_json(payload)
socket.send_json(payload)
print(json.dumps(socket.recv_json(), indent=4))
print(json.dumps(socket.recv_json(), indent=4))
