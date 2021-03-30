import TestRenderer, { act } from 'react-test-renderer'

import OrganizationUsersMenu from '../Menu'

const ORGANIZATION_ID = '42869703-fb62-4988-a0d1-e59b15caff06'
const OWNER_ID = 42

describe('<OrganizationUsersMenu />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    fetch.mockResponseOnce('{}')

    const component = TestRenderer.create(
      <OrganizationUsersMenu
        organizationId={ORGANIZATION_ID}
        ownerId={OWNER_ID}
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

    // Select Remove Owner
    act(() => {
      component.root.findByProps({ children: 'Remove Owner' }).props.onClick()
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

    // Select Remove Owner
    act(() => {
      component.root.findByProps({ children: 'Remove Owner' }).props.onClick()
    })

    // Confirm
    await act(async () => {
      component.root.findByProps({ children: 'Remove Owner' }).props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/organizations/${ORGANIZATION_ID}/owners/${OWNER_ID}/`,
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
