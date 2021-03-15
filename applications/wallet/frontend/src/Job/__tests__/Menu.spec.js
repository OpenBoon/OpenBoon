import TestRenderer, { act } from 'react-test-renderer'

import JobMenu from '../Menu'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '82d5308b-67c2-1433-8fef-0a580a000955'

describe('<JobMenu />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })
    const mockRevalidate = jest.fn()

    fetch.mockResponseOnce('{}')

    const component = TestRenderer.create(
      <JobMenu status="InProgress" revalidate={mockRevalidate} />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Dropdown Menu' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    await act(async () => {
      component.root.findByProps({ children: 'Pause' }).props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/jobs/${JOB_ID}/pause/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'PUT',
    })

    expect(mockRevalidate).toHaveBeenCalled()
  })
})
