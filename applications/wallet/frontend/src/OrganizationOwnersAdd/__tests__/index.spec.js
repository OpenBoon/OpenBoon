import TestRenderer, { act } from 'react-test-renderer'

import organizationOwnersAdd from '../__mocks__/organizationOwnersAdd'
import roles from '../../Roles/__mocks__/roles'

import OrganizationOwnersAdd from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Copy/helpers')

const noop = () => () => {}

describe('<OrganizationOwnersAdd />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/orgainzations/[organizationId]/owners/add',
      query: { organizationId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: roles,
    })

    const mockOnCopy = jest.fn()

    require('../../Copy/helpers').__setMockOnCopy(mockOnCopy)

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

    // Copy Key to clipboard
    act(() => {
      component.root
        .findByProps({ children: 'Copy Link' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockOnCopy).toHaveBeenCalledWith({ copyRef: { current: null } })

    // Reset form
    act(() => {
      component.root
        .findByProps({ children: 'Add More Owner(s)' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
