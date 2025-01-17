import TestRenderer from 'react-test-renderer'

import AccountPassword from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<AccountPassword />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/account/password',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<AccountPassword />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
