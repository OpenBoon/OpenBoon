import TestRenderer, { act } from 'react-test-renderer'

import projectUsersAdd from '../__mocks__/projectUsersAdd'
import roles from '../../Roles/__mocks__/roles'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import ProjectUsersAdd from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<ProjectUsersAdd />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/users/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: roles,
    })

    const mockCopyFn = jest.fn()

    window.navigator.clipboard.writeText = mockCopyFn

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ProjectUsersAdd />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Input email
    act(() => {
      component.root
        .findByProps({ name: 'emails' })
        .props.onChange({ target: { value: 'jane@email.com' } })
    })

    // Check role box
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'ML_Tools' })
        .props.onClick()
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
    fetch.mockResponseOnce(JSON.stringify(projectUsersAdd), {
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
    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Copy Link to Clipboard' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockCopyFn).toHaveBeenCalledWith('http://localhost/create-account')

    // Reset form
    act(() => {
      component.root
        .findByProps({ children: 'Add Another User' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
