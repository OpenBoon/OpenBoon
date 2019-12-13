import TestRenderer from 'react-test-renderer'

import DataQueue, { noop } from '..'

import { jobs } from '../__mocks__/jobs'

jest.mock('../../Pagination', () => 'Pagination')
jest.mock('../../UserMenu', () => 'UserMenu')

describe('<DataQueue />', () => {
  const selectedProject = {
    id: '76917058-b147-4556-987a-0a0f11e46d9b',
    name: 'qwerty',
  }

  it('should render properly while loading', () => {
    const component = TestRenderer.create(
      <DataQueue logout={noop} selectedProject={selectedProject} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no jobs', () => {
    require('swr').__setMockUseSWRResponse({
      data: { list: [] },
    })

    const component = TestRenderer.create(
      <DataQueue logout={noop} selectedProject={selectedProject} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with jobs', () => {
    require('swr').__setMockUseSWRResponse({
      data: { list: jobs.list },
    })

    const component = TestRenderer.create(
      <DataQueue logout={noop} selectedProject={selectedProject} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should do nothing on noop', () => {
    expect(noop()()).toBeUndefined()
  })
})
