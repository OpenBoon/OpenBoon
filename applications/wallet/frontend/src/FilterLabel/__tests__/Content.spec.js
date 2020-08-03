import TestRenderer, { act } from 'react-test-renderer'

import FilterLabelContent, { noop } from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = 'e004af1b-d6d6-1341-9e6a-a6d1aa55c7d8'

jest.mock('../../Filter/Reset', () => 'FilterReset')

describe('<FilterLabelContent />', () => {
  it('should select a label', () => {
    const filter = {
      type: 'label',
      attribute: 'labels.console',
      modelId: MODEL_ID,
      values: { labels: [] },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [
            { key: 'Tyngsboro' },
            { key: 'Brooklyn' },
            { key: 'Cát Bà' },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterLabelContent
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Tyngsboro' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                type: 'label',
                attribute: 'labels.console',
                modelId: MODEL_ID,
                values: { labels: ['Tyngsboro'] },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImxhYmVsIiwiYXR0cmlidXRlIjoibGFiZWxzLmNvbnNvbGUiLCJtb2RlbElkIjoiZTAwNGFmMWItZDZkNi0xMzQxLTllNmEtYTZkMWFhNTVjN2Q4IiwidmFsdWVzIjp7ImxhYmVscyI6WyJUeW5nc2Jvcm8iXX19XQ==',
    )
  })

  it('should unselect a label', () => {
    const filter = {
      type: 'label',
      attribute: 'labels.console',
      modelId: MODEL_ID,
      values: { labels: ['Tyngsboro'] },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        results: {
          buckets: [
            { key: 'Tyngsboro' },
            { key: 'Brooklyn' },
            { key: 'Cát Bà' },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterLabelContent
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
        .findByProps({ 'aria-label': 'Tyngsboro' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                type: 'label',
                attribute: 'labels.console',
                modelId: MODEL_ID,
                values: {},
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImxhYmVsIiwiYXR0cmlidXRlIjoibGFiZWxzLmNvbnNvbGUiLCJtb2RlbElkIjoiZTAwNGFmMWItZDZkNi0xMzQxLTllNmEtYTZkMWFhNTVjN2Q4IiwidmFsdWVzIjp7fX1d',
    )
  })

  it('should render with no data', () => {
    const filter = {
      type: 'label',
      attribute: 'labels.console',
      modelId: MODEL_ID,
      values: {},
    }

    require('swr').__setMockUseSWRResponse({})

    const component = TestRenderer.create(
      <FilterLabelContent
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

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
