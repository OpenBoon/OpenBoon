import TestRenderer from 'react-test-renderer'

import DataQueue, { noop } from '..'

import jobs from '../__mocks__/jobs'

jest.mock('../../Table', () => 'Table')
jest.mock('../../Pagination', () => 'Pagination')
jest.mock('../../UserMenu', () => 'UserMenu')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<DataQueue />', () => {
  it('should render properly while loading', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<DataQueue />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no jobs', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(<DataQueue />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with jobs', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobs,
    })

    const component = TestRenderer.create(<DataQueue />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should do nothing on noop', () => {
    expect(noop()()).toBeUndefined()
  })

  // describe('', () => {
  //   it('should properly render DataQueueRow', () => {
  //     require('swr').__setMockUseSWRResponse({
  //       data: jobs,
  //     })

  //     const component = TestRenderer.create(
  //       <DataQueue logout={noop} selectedProject={selectedProject} />,
  //     )

  //     const mockJob = jobs.results[0]
  //     expect(
  //       component.root.findByType('Table').props.renderRow(mockJob),
  //     ).toBeInstanceOf(<DataQueueRow key={mockJob.id} job={mockJob} />)
  //   })
  // })
})
