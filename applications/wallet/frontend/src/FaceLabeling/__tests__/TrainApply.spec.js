import TestRenderer from 'react-test-renderer'

import FaceLabelingTrainApply from '../TrainApply'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '1a1d45af-7477-1396-ae57-a618e8efb91f'

describe('<FaceLabelingTrainApply />', () => {
  it('should render properly when there are no unapplied changes and no training job', () => {
    require('swr').__setMockUseSWRResponse({
      data: { unappliedChanges: false, jobId: '' },
    })

    const component = TestRenderer.create(
      <FaceLabelingTrainApply projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when there are unapplied changes and a training job', () => {
    require('swr').__setMockUseSWRResponse({
      data: { unappliedChanges: true, jobId: JOB_ID },
    })

    const component = TestRenderer.create(
      <FaceLabelingTrainApply projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
