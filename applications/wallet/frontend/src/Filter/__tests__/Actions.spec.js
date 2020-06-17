import TestRenderer, { act } from 'react-test-renderer'

import FilterActions from '../Actions'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = ''

describe('<FilterActions />', () => {
  it('should render properly', () => {
    const filters = [{ attribute: 'clip.length', type: 'range' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterActions
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Disable filter
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Disable Filter' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    const encodedDisable = btoa(
      JSON.stringify([
        { attribute: 'clip.length', type: 'range', isDisabled: true },
      ]),
    )

    expect(component.toJSON()).toMatchSnapshot()
    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: encodedDisable,
        },
      },
      `/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=${encodedDisable}`,
    )

    // Delete filter
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Delete Filter' })
        .props.onClick({ preventDefault: noop, stopPropagation: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: '',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer',
    )
  })

  it('should render disabled properly', () => {
    const filters = [
      { attribute: 'clip.length', type: 'range', isDisabled: true },
    ]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterActions
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
