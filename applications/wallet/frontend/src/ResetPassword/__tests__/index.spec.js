import TestRenderer from 'react-test-renderer'

import ResetPassword from '..'

describe('<ResetPassword />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<ResetPassword />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
