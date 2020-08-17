import TestRenderer from 'react-test-renderer'

import Table from '..'

const noop = () => () => {}

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
        columns={['ColumnOne, ColumnTwo']}
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
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
