#!/usr/bin/env python3

import zmlp

images = ["../../../test-data/images/set03/BHP_SWC_PHOTOGRAPHY.TIF",
          "../../../test-data/images/set01/toucan.jpg"]
app = zmlp.app_from_env()
print(app.client.upload_files("/ml/v1/sim-hash", images, body=None))
