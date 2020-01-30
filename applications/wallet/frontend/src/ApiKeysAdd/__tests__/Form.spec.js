import TestRenderer, { act } from 'react-test-renderer'

import permissions from '../../ProjectPermissions/__mocks__/permissions'

import ApiKeysAddForm from '../Form'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../helpers')

const noop = () => () => {}

describe('<ApiKeysAddForm />', () => {
  it('should render properly after permissions are loaded', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/api-keys/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: permissions,
    })

    const mockOnCopy = jest.fn()

    require('../helpers').__setMockOnCopy(mockOnCopy)

    const component = TestRenderer.create(<ApiKeysAddForm />)

    expect(component.toJSON()).toMatchSnapshot()

    // Input API Key name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'API Key Name' } })
    })

    // Check permission box
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'ProjectManage' })
        .props.onClick()
    })

    // Submit form
    act(() => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Copy Key to clipboard
    act(() => {
      component.root
        .findByProps({ children: 'Copy Key' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockOnCopy).toHaveBeenCalledWith({ textareaRef: { current: null } })

    // Reset form
    act(() => {
      component.root
        .findByProps({ children: 'Create Another Key' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not POST the form', () => {
    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({
      data: permissions,
    })

    const component = TestRenderer.create(<ApiKeysAddForm />)

    component.root
      .findByProps({ method: 'post' })
      .props.onSubmit({ preventDefault: mockFn })

    expect(mockFn).toHaveBeenCalled()
  })
})
