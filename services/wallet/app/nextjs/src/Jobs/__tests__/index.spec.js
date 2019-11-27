import TestRenderer from 'react-test-renderer'

import Jobs from '../'

describe('<Jobs />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Jobs user={{ email: 'World' }} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
