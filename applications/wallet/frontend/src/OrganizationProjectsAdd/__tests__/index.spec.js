import TestRenderer, { act } from 'react-test-renderer'

import OrganizationProjectsAdd from '..'

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'
const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<OrganizationProjectsAdd />', () => {
  it('should render properly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/projects/add',
      query: { organizationId: ORGANIZATION_ID },
    })

    const component = TestRenderer.create(<OrganizationProjectsAdd />)

    // Input Project name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'Project Name' } })
    })

    // Mock Failure
    fetch.mockResponseOnce(JSON.stringify({ name: ['Name already in use'] }), {
      status: 400,
    })

    // Submit form
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Success
    fetch.mockResponseOnce(
      JSON.stringify({ id: PROJECT_ID, name: 'Project Name' }),
    )

    // Submit form
    await act(async () => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/organizations/[organizationId]?action=add-project-success',
      `/organizations/${ORGANIZATION_ID}`,
    )
  })
})
