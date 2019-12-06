import TestRenderer from 'react-test-renderer'

import Layout from '..'
import LayoutNavBar from '../NavBar'

describe('<Layout />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Layout>
        <LayoutNavBar />
      </Layout>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
