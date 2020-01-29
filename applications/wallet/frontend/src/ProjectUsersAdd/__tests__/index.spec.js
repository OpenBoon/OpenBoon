import TestRenderer from 'react-test-renderer'

import ProjectUsersAdd from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ProjectUsersAdd />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/users/add',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<ProjectUsersAdd />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
