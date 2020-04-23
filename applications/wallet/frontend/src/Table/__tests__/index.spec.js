import TestRenderer from 'react-test-renderer'

import Table from '..'

const noop = () => () => {}

describe('<Table />', () => {
  it('should render properly without data', () => {
    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <Table
        legend="Stuff"
        url=""
        refreshKeys={[]}
        columns={['ColumnOne, ColumnTwo']}
        expandColumn={2}
        renderEmpty="Empty"
        renderRow={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
