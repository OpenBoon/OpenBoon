import TestRenderer, { act } from 'react-test-renderer'

import JobErrorsMenu from '../Menu'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '82d5308b-67c2-1433-8fef-0a580a000955'

describe('<JobErrorsMenu />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    fetch.mockResponseOnce('{}')

    const component = TestRenderer.create(
      <JobErrorsMenu
        projectId={PROJECT_ID}
        jobId={JOB_ID}
        revalidate={mockFn}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    await act(async () => {
      component.root.findByProps({ children: 'Retry Task' }).props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/jobs/${JOB_ID}/retry/`,
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
