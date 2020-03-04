import TestRenderer from 'react-test-renderer'

import Loading from '..'

describe('<Loading />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Loading />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
