import TestRenderer from 'react-test-renderer'

import AuthenticationLoading from '../Loading'

describe('<AuthenticationLoading />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<AuthenticationLoading />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
