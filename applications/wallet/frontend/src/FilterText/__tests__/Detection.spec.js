import TestRenderer, { act } from 'react-test-renderer'

import FilterTextDetection from '../Detection'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filter/Reset', () => 'FilterReset')

const noop = () => () => {}

describe('<FilterTextDetection />', () => {
  it('should select a facet', () => {
    const filter = {
      type: 'textContent',
      attribute: 'analysis.boonai-text-content',
      values: {},
    }

    const mockFn = jest.fn()
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterTextDetection
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Attempt to submit without search
    act(() => {
      component.root.findByProps({ type: 'submit' }).props.onClick()
      component.root
        .findByProps({ method: 'post' })
        .props.onSubmit({ preventDefault: mockFn })
    })

    // Fill in search
    act(() => {
      component.root
        .findByProps({ type: 'search' })
        .props.onChange({ target: { value: 'cats' } })
    })

    // Submit search
    act(() => {
      component.root.findByProps({ type: 'submit' }).props.onClick()
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: 'analysis.boonai-text-content',
          values: { query: 'cats' },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should edit a text detection', () => {
    const filter = {
      type: 'textContent',
      attribute: 'analysis.boonai-text-content',
      values: { query: 'cats' },
    }

    const component = TestRenderer.create(
      <FilterTextDetection
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Edit Text Detection' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should clear a text detection', () => {
    const filter = {
      type: 'textContent',
      attribute: 'analysis.boonai-text-content',
      values: { query: 'cats' },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterTextDetection
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
        .findByProps({ 'aria-label': 'Clear Text Detection' })
        .props.onClick({ preventDefault: noop })
    })

    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: 'analysis.boonai-text-content',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })
})
