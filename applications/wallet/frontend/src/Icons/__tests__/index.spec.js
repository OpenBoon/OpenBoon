import TestRenderer from 'react-test-renderer'

import Icons from '..'

describe('<Icons />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Icons />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
