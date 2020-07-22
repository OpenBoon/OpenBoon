import TestRenderer from 'react-test-renderer'

import Loading from '..'

describe('<Loading />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Loading />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when transparent', () => {
    const component = TestRenderer.create(<Loading transparent />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
