import TestRenderer, { act } from 'react-test-renderer'

import projectUsers from '../__mocks__/projectUsers'

import ProjectUsersMenu from '../Menu'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ProjectUsersMenu />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    fetch.mockResponseOnce('{}')

    const component = TestRenderer.create(
      <ProjectUsersMenu
        projectId={PROJECT_ID}
        user={projectUsers.results[0]}
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

    // Select Remove
    act(() => {
      component.root.findByProps({ children: 'Remove' }).props.onClick()
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

    // Select Remove
    act(() => {
      component.root.findByProps({ children: 'Remove' }).props.onClick()
    })

    // Confirm
    await act(async () => {
      component.root.findByProps({ children: 'Remove User' }).props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/users/${projectUsers.results[0].id}/`,
    )

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
