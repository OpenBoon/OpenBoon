import TestRenderer, { act } from 'react-test-renderer'

import organizationOwnersAdd from '../__mocks__/organizationOwnersAdd'

import OrganizationOwnersAdd from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<OrganizationOwnersAdd />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/organizations/[organizationId]/owners/add',
      query: { organizationId: PROJECT_ID },
    })

    const component = TestRenderer.create(<OrganizationOwnersAdd />)

    expect(component.toJSON()).toMatchSnapshot()

    // Input email
    act(() => {
      component.root
        .findByProps({ name: 'emails' })
        .props.onChange({ target: { value: 'jane@email.com' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Add' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify(organizationOwnersAdd), {
      headers: { 'content-type': 'application/json' },
    })

    // Submit the form
    await act(async () => {
      component.root
        .findByProps({ children: 'Add' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Reset form
    act(() => {
      component.root
        .findByProps({ children: 'Add More Owner(s)' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
