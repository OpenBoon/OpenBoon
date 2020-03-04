import TestRenderer, { act } from 'react-test-renderer'

import permissions from '../../Permissions/__mocks__/permissions'

import ProjectUsersAdd from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../helpers')

const noop = () => () => {}

describe('<ProjectUsersAdd />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/users/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: permissions,
    })

    const mockOnCopy = jest.fn()

    require('../helpers').__setMockOnCopy(mockOnCopy)

    const component = TestRenderer.create(<ProjectUsersAdd />)

    expect(component.toJSON()).toMatchSnapshot()

    // Input email
    act(() => {
      component.root
        .findByProps({ name: 'emails' })
        .props.onChange({ target: { value: 'jane@email.com' } })
    })

    // Check permission box
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'SystemProjectOverride' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Submit the form
    act(() => {
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

    expect(mockOnCopy).toHaveBeenCalledWith({ inputRef: { current: null } })

    // Reset form
    act(() => {
      component.root
        .findByProps({ children: 'Add Another User' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
