import TestRenderer from 'react-test-renderer'

import Layout from '..'

jest.mock('../NavBar', () => 'NavBar')

describe('<Layout />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Layout>
        <div />
      </Layout>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
