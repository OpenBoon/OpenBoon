import TestRenderer, { act } from 'react-test-renderer'

import job from '../__mocks__/job'

import JobDetails from '../Details'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '82d5308b-67c2-1433-8fef-0a580a000956'

const noop = () => () => {}

describe('<JobDetails />', () => {
  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, jobId: JOB_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...job, state: 'InProgress' },
      mutate: noop,
    })

    const component = TestRenderer.create(<JobDetails />)

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Dropdown Menu' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    await act(async () => {
      component.root.findByProps({ children: 'Pause' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with an action confirmation', async () => {
    require('next/router').__setUseRouter({
      query: {
        projectId: PROJECT_ID,
        jobId: JOB_ID,
        action: 'Retry All Failures',
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: job,
      mutate: noop,
    })

    const component = TestRenderer.create(<JobDetails />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
