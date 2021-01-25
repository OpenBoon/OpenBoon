import TestRenderer from 'react-test-renderer'

import ProjectMetrics from '../Metrics'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<ProjectMetrics />', () => {
  it('should render properly when data is unavailable', () => {
    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <ProjectMetrics projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when data is available', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        tier_1: {
          image_count: 2,
          video_minutes: 4.6,
        },
        tier_2: {
          image_count: 8,
          video_minutes: 16.32,
        },
        image_count: 64,
        video_hours: 128,
      },
    })

    const component = TestRenderer.create(
      <ProjectMetrics projectId={PROJECT_ID} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
