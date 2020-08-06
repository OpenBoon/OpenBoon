import TestRenderer from 'react-test-renderer'

import MetadataPrettyLabelsRow from '../LabelsRow'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

describe('<MetadataPrettyLabelsRow />', () => {
  it('should render properly with bbox', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        'zvi-console-face-recognition': {
          count: 1,
          type: 'labels',
          predictions: [
            {
              score: 1.0,
              bbox: [0.226, 0.327, 0.697, 0.813],
              label: 'Mark Ruffalo',
              b64_image: 'data:image/png;base64, iVBORw0KGgoAA...',
            },
          ],
        },
      },
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsRow
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        models={[
          {
            id: 'b030033c-c904-11ea-8966-d27a587a06a5',
            name: 'console',
            moduleName: 'zvi-console-face-recognition',
          },
        ]}
        label={{
          modelId: 'b030033c-c904-11ea-8966-d27a587a06a5',
          scope: 'TRAIN',
          bbox: [0.226, 0.327, 0.697, 0.813],
          label: 'Mark Ruffalo',
          simhash: 'NNLHLINNMQPONMLMJFLMQMK...',
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly without bbox', () => {
    require('swr').__setMockUseSWRResponse({})

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsRow
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        models={[
          {
            id: 'b030033c-c904-11ea-8966-d27a587a06a5',
            name: 'console',
            moduleName: 'zvi-console-face-recognition',
          },
        ]}
        label={{
          modelId: 'b030033c-c904-11ea-8966-d27a587a06a5',
          scope: 'TRAIN',
          label: 'Mark Ruffalo',
          simhash: 'NNLHLINNMQPONMLMJFLMQMK...',
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
