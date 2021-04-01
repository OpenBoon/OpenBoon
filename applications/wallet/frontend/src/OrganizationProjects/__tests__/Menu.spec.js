import TestRenderer, { act } from 'react-test-renderer'

import OrganizationProjectsMenu from '../Menu'

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'
const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<OrganizationProjectsMenu />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    fetch.mockResponseOnce('{}')

    const component = TestRenderer.create(
      <OrganizationProjectsMenu
        organizationId={ORGANIZATION_ID}
        projectId={PROJECT_ID}
        revalidate={mockFn}
      />,
    )

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Select Delete Project
    act(() => {
      component.root.findByProps({ children: 'Delete Project' }).props.onClick()
    })

    // Cancel
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Delete Project
    act(() => {
      component.root.findByProps({ children: 'Delete Project' }).props.onClick()
    })

    // Confirm
    await act(async () => {
      component.root
        .findByProps({ children: 'Delete Permanently' })
        .props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(`/api/v1/projects/${PROJECT_ID}/`)

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'DELETE',
    })

    expect(mockFn).toHaveBeenCalledWith()
  })
})
