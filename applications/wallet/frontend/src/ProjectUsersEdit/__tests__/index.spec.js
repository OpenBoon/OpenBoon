import TestRenderer from 'react-test-renderer'

import ProjectUsersEdit from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const USER_ID = 'fe39c66b-68f8-4d59-adfd-395f6baaf72c'

describe('<ProjectUsersEdit />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/users/[userId]/edit',
      query: { projectId: PROJECT_ID, userId: USER_ID },
    })

    const component = TestRenderer.create(<ProjectUsersEdit />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
