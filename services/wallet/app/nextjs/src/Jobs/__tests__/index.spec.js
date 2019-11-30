import TestRenderer from 'react-test-renderer'

import Jobs from '../'

const noop = () => () => { }
jest.mock('../__mocks__/jobs')

describe('<Jobs />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Jobs logout={noop} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
