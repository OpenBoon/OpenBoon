import TestRenderer, { act } from 'react-test-renderer'

import permissions from '../../Permissions/__mocks__/permissions'

import ApiKeysAddForm from '../Form'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../helpers', () => ({
  onSubmit: ({ dispatch }) =>
    dispatch({
      apikey: { permissions: ['ApiKeyManage'], secretKey: 'FooBarSecretKey' },
    }),
}))

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

    const component = TestRenderer.create(<ApiKeysAddForm />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'API Key Name' } })
    })

    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'ProjectManage' })
        .props.onClick()
    })

    act(() => {
      component.root
        .findByProps({ type: 'submit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

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
