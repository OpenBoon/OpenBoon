import TestRenderer, { act } from 'react-test-renderer'

import FilterLabelConfidenceContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Slider', () => 'Slider')
jest.mock('../../Filter/Reset', () => 'FilterReset')

const noop = () => () => {}

describe('<FilterLabelConfidenceContent />', () => {
  it('should select a label', () => {
    const filter = {
      type: 'labelConfidence',
      attribute: 'analysis.boonai-label-detection',
      values: {},
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [
            { key: 'web_site', docCount: 134 },
            { key: 'alp', docCount: 75 },
            { key: 'sports_car', docCount: 56 },
            { key: 'menu', docCount: 45 },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterLabelConfidenceContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Search
    act(() => {
      component.root
        .findByProps({ type: 'search' })
        .props.onChange({ target: { value: 'web' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Select
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'web_site' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'labelConfidence',
          attribute: 'analysis.boonai-label-detection',
          values: { labels: ['web_site'], min: 0, max: 1 },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should unselect a label', () => {
    const filter = {
      type: 'labelConfidence',
      attribute: 'analysis.boonai-label-detection',
      values: { labels: ['web_site'], min: 0.0, max: 1.0 },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [
            { key: 'web_site' },
            { key: 'alp' },
            { key: 'sports_car' },
            { key: 'menu' },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterLabelConfidenceContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'web_site' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'labelConfidence',
          attribute: 'analysis.boonai-label-detection',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should update the confidence range', () => {
    const filter = {
      type: 'labelConfidence',
      attribute: 'analysis.boonai-label-detection',
      values: { labels: ['web_site'], min: 0.0, max: 1.0 },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [
            { key: 'web_site' },
            { key: 'alp' },
            { key: 'sports_car' },
            { key: 'menu' },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterLabelConfidenceContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    act(() => {
      component.root.findByType('Slider').props.onUpdate([0.2, 0.8])
    })

    act(() => {
      component.root.findByType('Slider').props.onChange([0.2, 0.8])
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'labelConfidence',
          attribute: 'analysis.boonai-label-detection',
          values: { labels: ['web_site'], min: 0.2, max: 0.8 },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )

    act(() => {
      component.root.findByType('FilterReset').props.onReset()
    })
  })

  it('should render with no data', () => {
    const filter = {
      type: 'labelConfidence',
      attribute: 'analysis.boonai-label-detection',
      values: { labels: [], min: 0.0, max: 1.0 },
    }

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <FilterLabelConfidenceContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
