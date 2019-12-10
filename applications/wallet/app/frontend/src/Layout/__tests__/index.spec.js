import TestRenderer from 'react-test-renderer'

import Layout from '..'

jest.mock('../../ProjectSwitcher', () => 'ProjectSwitcher')

describe('<Layout />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Layout>{() => `Hello World`}</Layout>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
