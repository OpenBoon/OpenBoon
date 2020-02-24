import TestRenderer from 'react-test-renderer'

import JobError from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '223fd17d-7028-1519-94a8-d2f0132bc0c8'
const ERROR_ID = '916c86bc-74b9-1519-b065-d2f0132bc0c8'

describe('<JobError />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, errorId: ERROR_ID },
    })

    const component = TestRenderer.create(<JobError />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
