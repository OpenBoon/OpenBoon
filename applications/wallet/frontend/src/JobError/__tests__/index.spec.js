import TestRenderer, { act } from 'react-test-renderer'

import JobError from '..'
import { jobErrorFatal, jobErrorNonFatal } from '../__mocks__/jobError'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = jobErrorFatal.jobId
const FATAL_ERROR_ID = jobErrorFatal.id
const NON_FATAL_ERROR_ID = jobErrorNonFatal.id

describe('<JobError />', () => {
  it('should render properly with a fatal error', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: { projectId: PROJECT_ID, jobId: JOB_ID, errorId: FATAL_ERROR_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorFatal,
    })

    const component = TestRenderer.create(<JobError />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a non-fatal error', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]',
      query: {
        projectId: PROJECT_ID,
        jobId: JOB_ID,
        errorId: NON_FATAL_ERROR_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorNonFatal,
    })

    const component = TestRenderer.create(<JobError />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Dropdown Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    await act(async () => {
      component.root.findByProps({ children: 'Retry Task' }).props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/tasks/${jobErrorNonFatal.taskId}/retry/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'PUT',
    })
  })

  it('should render asset view properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/errors/[errorId]/asset',
      query: {
        projectId: PROJECT_ID,
        errorId: FATAL_ERROR_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobErrorFatal,
    })

    const component = TestRenderer.create(<JobError />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
