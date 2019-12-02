import TestRenderer from 'react-test-renderer'
import DataQueue from '../'

jest.mock('../__mocks__/jobs')

describe('<Jobs />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<DataQueue />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
