import TestRenderer from 'react-test-renderer'

import Layout from '..'

describe('<Layout />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Layout results={[{ url: '1', name: 'project-name' }]}>
        {() => 'Hello world'}
      </Layout>
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
