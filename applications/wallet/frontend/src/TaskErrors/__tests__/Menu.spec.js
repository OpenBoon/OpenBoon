import TestRenderer, { act } from 'react-test-renderer'

import TaskErrorsMenu from '../Menu'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const TASK_ID = '5262c1ef-91ad-1d33-82b6-d6edb1b855c4'

describe('<TaskErrorsMenu />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    fetch.mockResponseOnce('{}')

    const component = TestRenderer.create(
      <TaskErrorsMenu
        projectId={PROJECT_ID}
        taskId={TASK_ID}
        revalidate={mockFn}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    await act(async () => {
      component.root
        .findByProps({ children: 'Retry Task' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/tasks/${TASK_ID}/retry/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'PUT',
    })

    expect(mockFn).toHaveBeenCalledWith()
  })
})
