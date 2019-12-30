import TestRenderer from 'react-test-renderer'

import Table from '..'

const noop = () => () => {}

describe('<Table />', () => {
  it('should render properly without data', () => {
    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <Table
        url=""
        columns={['ColumnOne, ColumnTwo']}
        renderEmpty="Empty"
        renderRow={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
