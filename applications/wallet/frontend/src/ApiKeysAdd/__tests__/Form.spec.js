import TestRenderer, { act } from 'react-test-renderer'

import permissions from '../../Permissions/__mocks__/permissions'

import ApiKeysAddForm from '../Form'

const noop = () => () => {}

describe('<ApiKeysAddForm />', () => {
  it('should render properly after permissions are loaded', () => {
    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({
      data: permissions,
    })

    const component = TestRenderer.create(<ApiKeysAddForm onSubmit={mockFn} />)

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

    expect(mockFn).toHaveBeenCalledWith({
      name: 'API Key Name',
      permissions: { ProjectManage: true },
    })
  })

  it('should not POST the form', () => {
    const mockFn = jest.fn()
    const mockOnSubmit = jest.fn()

    require('swr').__setMockUseSWRResponse({
      data: permissions,
    })

    const component = TestRenderer.create(
      <ApiKeysAddForm onSubmit={mockOnSubmit} />,
    )

    component.root
      .findByProps({ method: 'post' })
      .props.onSubmit({ preventDefault: mockFn })

    expect(mockOnSubmit).not.toHaveBeenCalled()
    expect(mockFn).toHaveBeenCalled()
  })
})
