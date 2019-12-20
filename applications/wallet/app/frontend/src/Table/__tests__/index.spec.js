import TestRenderer from 'react-test-renderer'

import Table from '..'

describe('<Table />', () => {
  it('should render properly with no items', () => {
    const component = TestRenderer.create(
      <Table
        columns={['Foo', 'Bar']}
        items={[]}
        renderRow={item => (
          <tr key={item.foo}>
            <td>{item.foo}</td>
            <td>{item.bar}</td>
          </tr>
        )}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with items', () => {
    const component = TestRenderer.create(
      <Table
        columns={['Foo', 'Bar']}
        items={[
          { foo: 'a', bar: 'b' },
          { foo: 'c', bar: 'd' },
        ]}
        renderRow={item => (
          <tr key={item.foo}>
            <td>{item.foo}</td>
            <td>{item.bar}</td>
          </tr>
        )}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
