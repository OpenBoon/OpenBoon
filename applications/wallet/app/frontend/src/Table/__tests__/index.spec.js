import TestRenderer from 'react-test-renderer'

import Table from '..'

describe('<Table />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Table rows={[]} columns={[]} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
