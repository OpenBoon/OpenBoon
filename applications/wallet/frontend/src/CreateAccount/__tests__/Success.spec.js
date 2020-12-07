import TestRenderer from 'react-test-renderer'

import CreateAccountSuccess, { noop } from '../Success'

describe('<CreateAccountSuccess />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<CreateAccountSuccess />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
