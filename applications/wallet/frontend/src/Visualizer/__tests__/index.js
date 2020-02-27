import TestRenderer from 'react-test-renderer'

import Assets from '..'

describe('<Assets />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Assets />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
