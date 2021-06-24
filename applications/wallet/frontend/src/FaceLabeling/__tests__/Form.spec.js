import TestRenderer from 'react-test-renderer'

import FaceLabelingForm from '../Form'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

jest.mock('../../Combobox', () => 'Combobox')

describe('<FaceLabelingForm />', () => {
  it('should render with no data', () => {
    require('swr').__setMockUseSWRResponse({})

    const predictions = [
      {
        score: 0.999,
        bbox: [0.38, 0.368, 0.484, 0.584],
        label: 'face1',
        simhash: 'MNONPMMKPLRLONLJMRLNM',
        b64Image: 'data:image/png;base64',
      },
    ]

    const component = TestRenderer.create(
      <FaceLabelingForm
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        predictions={predictions}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
