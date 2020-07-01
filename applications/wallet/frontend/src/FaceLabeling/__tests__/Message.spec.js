import TestRenderer from 'react-test-renderer'

import FaceLabelingMessage from '../Message'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '1a1d45af-7477-1396-ae57-a618e8efb91f'

describe('<FaceLabelingMessage />', () => {
  it('should render properly when there there is a training job', () => {
    const component = TestRenderer.create(
      <FaceLabelingMessage
        projectId={PROJECT_ID}
        previousJobId=""
        currentJobId={JOB_ID}
        error=""
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when there was a previous training job but no current job', () => {
    const component = TestRenderer.create(
      <FaceLabelingMessage
        projectId={PROJECT_ID}
        previousJobId={JOB_ID}
        currentJobId=""
        error=""
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when there is no previous or current job', () => {
    const component = TestRenderer.create(
      <FaceLabelingMessage
        projectId={PROJECT_ID}
        previousJobId=""
        currentJobId=""
        error=""
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when there is an error', () => {
    const component = TestRenderer.create(
      <FaceLabelingMessage
        projectId={PROJECT_ID}
        previousJobId=""
        currentJobId=""
        error="Error"
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
