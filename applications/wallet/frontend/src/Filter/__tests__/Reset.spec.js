import TestRenderer, { act } from 'react-test-renderer'

import fields from '../../Filters/__mocks__/fields'
import asset from '../../Asset/__mocks__/asset'

import FilterReset from '../Reset'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = ''

describe('<FilterReset />', () => {
  it('should reset properly', () => {
    const filters = [{ attribute: 'clip.length', type: 'range' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(
      <FilterReset
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
        onReset={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Reset' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalled()

    const query = btoa(
      JSON.stringify([
        {
          attribute: 'clip.length',
          type: 'range',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should switch properly', () => {
    const filters = [{ attribute: 'clip.length', type: 'range' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(
      <FilterReset
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
        onReset={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ defaultValue: 'range' })
        .props.onChange({ target: { value: 'exists' } })
    })

    expect(mockFn).toHaveBeenCalled()

    const query = btoa(
      JSON.stringify([
        {
          attribute: 'clip.length',
          type: 'exists',
          values: { exists: true },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should switch properly', () => {
    const filters = [{ attribute: 'clip.length', type: 'exists' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(
      <FilterReset
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
        onReset={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ defaultValue: 'exists' })
        .props.onChange({ target: { value: 'range' } })
    })

    expect(mockFn).toHaveBeenCalled()

    const query = btoa(
      JSON.stringify([
        {
          attribute: 'clip.length',
          type: 'range',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })

  it('should render properly with no fields', () => {
    const filters = [{ attribute: 'clip.length', type: 'range' }]
    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <FilterReset
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
        onReset={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not switch from exists back to similarity', () => {
    const filters = [
      { attribute: 'analysis.boonai-image-similarity', type: 'exists' },
    ]
    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(
      <FilterReset
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
        onReset={mockFn}
      />,
    )

    expect(component.toJSON()).toBeNull()
  })

  it('should maintain "ids" when resetting', () => {
    const filters = [
      {
        type: 'similarity',
        attribute: 'analysis.boonai-image-similarity',
        values: { ids: [asset.id] },
      },
    ]
    const mockFn = jest.fn()
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)
    require('swr').__setMockUseSWRResponse({ data: {} })

    const component = TestRenderer.create(
      <FilterReset
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={asset.id}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
        onReset={mockFn}
      />,
    )

    const query = btoa(JSON.stringify(filters))

    act(() => {
      component.root
        .findByProps({ children: 'Reset' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${asset.id}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${asset.id}&query=${query}`,
    )
  })
})
