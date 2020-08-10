import TestRenderer, { act } from 'react-test-renderer'

import aggregate from '../__mocks__/aggregate'

import FilterLabelContent, { noop } from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf774-89d9-1244-9596-d6df43f1ede5'

jest.mock('../../Filter/Reset', () => 'FilterReset')

describe('<FilterLabelContent />', () => {
  it('should switch the scope', () => {
    const filter = {
      type: 'label',
      attribute: 'labels.console',
      modelId: MODEL_ID,
      values: { labels: ['Ford'] },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: aggregate })

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

    // Select scope
    act(() => {
      component.root
        .findByProps({ label: 'Scope' })
        .props.onChange({ value: 'train' })
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
                values: { labels: ['Ford'], scope: 'train' },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImxhYmVsIiwiYXR0cmlidXRlIjoibGFiZWxzLmNvbnNvbGUiLCJtb2RlbElkIjoiNjIxYmY3NzQtODlkOS0xMjQ0LTk1OTYtZDZkZjQzZjFlZGU1IiwidmFsdWVzIjp7ImxhYmVscyI6WyJGb3JkIl0sInNjb3BlIjoidHJhaW4ifX1d',
    )
  })

  it('should select a label', () => {
    const filter = {
      type: 'label',
      attribute: 'labels.console',
      modelId: MODEL_ID,
      values: { labels: [] },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: aggregate })

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
        .findByProps({ 'aria-label': 'Ford' })
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
                values: { scope: 'all', labels: ['Ford'] },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImxhYmVsIiwiYXR0cmlidXRlIjoibGFiZWxzLmNvbnNvbGUiLCJtb2RlbElkIjoiNjIxYmY3NzQtODlkOS0xMjQ0LTk1OTYtZDZkZjQzZjFlZGU1IiwidmFsdWVzIjp7InNjb3BlIjoiYWxsIiwibGFiZWxzIjpbIkZvcmQiXX19XQ==',
    )
  })

  it('should unselect a label', () => {
    const filter = {
      type: 'label',
      attribute: 'labels.console',
      modelId: MODEL_ID,
      values: { labels: ['Ford'] },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({ data: aggregate })

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
        .findByProps({ 'aria-label': 'Ford' })
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
                values: { scope: 'all', labels: [] },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3sidHlwZSI6ImxhYmVsIiwiYXR0cmlidXRlIjoibGFiZWxzLmNvbnNvbGUiLCJtb2RlbElkIjoiNjIxYmY3NzQtODlkOS0xMjQ0LTk1OTYtZDZkZjQzZjFlZGU1IiwidmFsdWVzIjp7InNjb3BlIjoiYWxsIiwibGFiZWxzIjpbXX19XQ==',
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
