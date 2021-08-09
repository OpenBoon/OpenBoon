import TestRenderer, { act } from 'react-test-renderer'

import Table from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Fetch/Ahead', () => 'FetchAhead')

describe('<Table />', () => {
  it('should render properly without data', () => {
    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <Table
        legend="Stuff"
        url=""
        refreshKeys={[]}
        refreshButton={false}
        columns={['ColumnOne', 'ColumnTwo', '#Checkmark#', '#Actions#']}
        expandColumn={2}
        renderEmpty="Empty"
        renderRow={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should prefetch previous and next page', () => {
    require('next/router').__setUseRouter({
      query: { page: 2 },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 100,
        results: [],
        previous: 'https://boonai.app/api/v1/stuff?from=0&size=20',
        next: 'https://boonai.app/api/v1/stuff?from=40&size=20',
      },
    })

    const component = TestRenderer.create(
      <Table
        legend="Stuff"
        url="/api/v1/stuff"
        refreshKeys={[]}
        refreshButton={false}
        columns={['ColumnOne, ColumnTwo']}
        expandColumn={2}
        renderEmpty="Empty"
        renderRow={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should filter', () => {
    const mockRouterPush = jest.fn()
    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID, sort: 'timeCreated' },
    })

    require('swr').__setMockUseSWRResponse({
      data: { count: 100, results: [] },
    })

    const component = TestRenderer.create(
      <Table
        legend="Stuff"
        url="/api/v1/stuff"
        refreshKeys={[]}
        refreshButton={false}
        columns={['ColumnOne, ColumnTwo']}
        expandColumn={2}
        renderEmpty="Empty"
        renderRow={noop}
        searchLabel="Job Name"
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Filter Job Name' })
        .props.onChange({ value: 'apply' })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/jobs?search=apply&sort=timeCreated',
      `/${PROJECT_ID}/jobs?search=apply&sort=timeCreated`,
    )
  })
})
