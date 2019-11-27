import TestRenderer from 'react-test-renderer'

import Login from '../'

const noop = () => () => {}

describe('<Login />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Login onSubmit={noop} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
