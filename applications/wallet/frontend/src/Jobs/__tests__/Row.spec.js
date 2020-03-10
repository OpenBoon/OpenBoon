import TestRenderer, { act } from 'react-test-renderer'

import JobsRow from '../Row'

import job from '../../Job/__mocks__/job'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<JobsRow />', () => {
  it('should navigate on a click on the row directly', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <JobsRow projectId={PROJECT_ID} job={job} revalidate={noop} />,
    )

    act(() => {
      component.root.findByType('tr').props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/jobs/[jobId]',
      `/${PROJECT_ID}/jobs/${job.id}`,
    )
  })

  it('should not navigate on a click on a link', async () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <JobsRow projectId={PROJECT_ID} job={job} revalidate={noop} />,
    )

    act(() => {
      component.root
        .findByType('tr')
        .props.onClick({ target: { localName: 'a' } })
    })

    expect(mockRouterPush).not.toHaveBeenCalled()
  })
})
