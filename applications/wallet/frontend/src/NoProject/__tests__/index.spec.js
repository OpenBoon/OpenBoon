import TestRenderer from 'react-test-renderer'

import NoProject from '..'

describe('<NoProject />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<NoProject />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
