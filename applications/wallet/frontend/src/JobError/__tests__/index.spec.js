import TestRenderer, { act } from 'react-test-renderer'

import jobError from '../__mocks__/jobError'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import JobError from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = jobError.jobId
const TASK_ID = jobError.taskId
const ERROR_ID = jobError.id

jest.mock('../../JobErrorAsset', () => 'JobErrorAsset')
jest.mock('../../JsonDisplay', () => 'JsonDisplay')

describe('<JobError />', () => {
  it('should render properly with a fatal error', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]',
      query: {
        projectId: PROJECT_ID,
        jobId: JOB_ID,
        taskId: TASK_ID,
        errorId: ERROR_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobError,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <JobError />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with a non-fatal error', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]',
      query: {
        projectId: PROJECT_ID,
        jobId: JOB_ID,
        taskId: TASK_ID,
        errorId: jobError.id,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...jobError, fatal: false },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <JobError />
      </User>,
    )

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
      `/api/v1/projects/${PROJECT_ID}/tasks/${TASK_ID}/retry/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'PUT',
    })
  })

  it('should render properly with an asset', () => {
    require('next/router').__setUseRouter({
      pathname:
        '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]/asset',
      query: {
        projectId: PROJECT_ID,
        jobId: JOB_ID,
        taskId: TASK_ID,
        errorId: ERROR_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: jobError,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <JobError />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
