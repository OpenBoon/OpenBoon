import TestRenderer from 'react-test-renderer'

import ApiKeys from '..'

describe('<ApiKeys />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<ApiKeys />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
