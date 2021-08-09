import TestRenderer, { act } from 'react-test-renderer'

import jobs from '../__mocks__/jobs'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Jobs from '..'

jest.mock('../../Pagination', () => 'Pagination')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Jobs />', () => {
  it('should render properly with no jobs', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID, sort: 'timeCreated' },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Jobs />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with jobs', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: {
        projectId: PROJECT_ID,
        sort: '-timeCreated',
        filters: btoa(JSON.stringify({ states: ['Cancelled'] })),
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobs,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Jobs />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Dropdown Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ value: 'Cancelled' }).props.onClick()
    })

    expect(mockRouterPush).toHaveBeenLastCalledWith(
      `/[projectId]/jobs?sort=-timeCreated`,
      `/${PROJECT_ID}/jobs?sort=-timeCreated`,
    )

    act(() => {
      component.root.findByProps({ value: 'InProgress' }).props.onClick()
    })

    const filters = btoa(
      JSON.stringify({ states: ['Cancelled', 'InProgress'] }),
    )

    expect(mockRouterPush).toHaveBeenLastCalledWith(
      `/[projectId]/jobs?filters=${filters}&sort=-timeCreated`,
      `/${PROJECT_ID}/jobs?filters=${filters}&sort=-timeCreated`,
    )
  })

  it('should render properly with jobs and no filter', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: {
        projectId: PROJECT_ID,
        sort: '-timeCreated',
        filters: btoa(JSON.stringify({ states: ['Cancelled', 'Archived'] })),
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobs,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Jobs />
      </User>,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Dropdown Menu' })
        .props.onClick()
    })

    act(() => {
      component.root.findByProps({ value: 'Cancelled' }).props.onClick()
    })

    const filters = btoa(JSON.stringify({ states: ['Archived'] }))

    expect(mockRouterPush).toHaveBeenLastCalledWith(
      `/[projectId]/jobs?filters=${filters}&sort=-timeCreated`,
      `/${PROJECT_ID}/jobs?filters=${filters}&sort=-timeCreated`,
    )
  })
})
