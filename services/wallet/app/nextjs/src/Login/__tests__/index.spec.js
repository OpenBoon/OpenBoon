import TestRenderer from 'react-test-renderer'

import Login from '../'

describe('<Login />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Login />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
