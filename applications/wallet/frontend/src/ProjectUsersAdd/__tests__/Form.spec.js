import TestRenderer, { act } from 'react-test-renderer'

import permissions from '../../ProjectUsersPermissions/__mocks__/permissions'

import ProjectUsersAddForm from '../Form'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ProjectUsersAddForm />', () => {
  it('should render properly after permissions are loaded', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/users/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: permissions,
    })

    const component = TestRenderer.create(<ProjectUsersAddForm />)

    expect(component.toJSON()).toMatchSnapshot()

    // Input email
    act(() => {
      component.root
        .findByProps({ id: 'email' })
        .props.onChange({ target: { value: 'jane@email.com' } })
    })

    // Check permission box
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'ManageApiKeys' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
