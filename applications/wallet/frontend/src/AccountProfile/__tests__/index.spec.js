import TestRenderer from 'react-test-renderer'

import AccountProfile from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<AccountProfile />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys/add',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<AccountProfile />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
