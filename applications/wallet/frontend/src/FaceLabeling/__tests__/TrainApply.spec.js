import TestRenderer from 'react-test-renderer'

import FaceLabelingTrainApply from '../TrainApply'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<FaceLabelingTrainApply />', () => {
  it('should render properly when there are no unapplied changes', () => {
    require('swr').__setMockUseSWRResponse({
      data: { unappliedChanges: false },
    })

    const component = TestRenderer.create(
      <FaceLabelingTrainApply projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
