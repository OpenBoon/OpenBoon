import TestRenderer, { act } from 'react-test-renderer'

import JobErrorsRow from '../Row'

import jobErrors from '../__mocks__/jobErrors'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = 'c097596f-62ef-1f81-83f8-0a580a000954'
const ERROR = jobErrors.results[0]

const noop = () => () => {}

describe('<JobErrorsRow />', () => {
  it('should navigate on a click on the row directly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <JobErrorsRow
        projectId={PROJECT_ID}
        jobId={JOB_ID}
        error={ERROR}
        revalidate={noop}
      />,
    )

    act(() => {
      component.root.findByType('tr').props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/jobs/[jobId]/errors/[errorId]',
      `/${PROJECT_ID}/jobs/${JOB_ID}/errors/${ERROR.id}`,
    )
  })

  it('should not navigate on a click on a link', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <JobErrorsRow
        projectId={PROJECT_ID}
        jobId={JOB_ID}
        error={ERROR}
        revalidate={noop}
      />,
    )

    act(() => {
      component.root
        .findByType('tr')
        .props.onClick({ target: { localName: 'a' } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()
  })
})
