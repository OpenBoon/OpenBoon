import TestRenderer from 'react-test-renderer'

import JobQueue, { noop } from '..'

import mockProjects from '../../Projects/__mocks__/projects'
import jobs from '../__mocks__/jobs'

jest.mock('../../Pagination', () => 'Pagination')
jest.mock('../../UserMenu', () => 'UserMenu')

describe('<JobQueue />', () => {
  const selectedProject = mockProjects.results[0]

  it('should render properly while loading', () => {
    const component = TestRenderer.create(
      <JobQueue logout={noop} selectedProject={selectedProject} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no jobs', () => {
    require('swr').__setMockUseSWRResponse({
      data: { results: [] },
    })

    const component = TestRenderer.create(
      <JobQueue logout={noop} selectedProject={selectedProject} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with jobs', () => {
    require('swr').__setMockUseSWRResponse({
      data: jobs,
    })

    const component = TestRenderer.create(
      <JobQueue logout={noop} selectedProject={selectedProject} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should do nothing on noop', () => {
    expect(noop()()).toBeUndefined()
  })
})
