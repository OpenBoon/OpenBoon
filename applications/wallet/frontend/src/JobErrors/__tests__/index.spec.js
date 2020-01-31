import TestRenderer from 'react-test-renderer'

import JobErrors from '..'

jest.mock('../Content', () => 'JobErrorsContent')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = 'c097596f-62ef-1f81-83f8-0a580a000954'

describe('<JobErrors />', () => {
  require('next/router').__setUseRouter({
    pathname: '/[projectId]/jobs/[jobId]/errors',
    query: { projectId: PROJECT_ID, jobId: JOB_ID },
  })

  it('should render properly', () => {
    const component = TestRenderer.create(<JobErrors />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
