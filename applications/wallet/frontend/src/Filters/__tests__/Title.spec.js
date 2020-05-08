import TestRenderer, { act } from 'react-test-renderer'

import FiltersTitle from '../Title'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = ''

describe('<FiltersTitle />', () => {
  it('should render properly', () => {
    const filters = [{ attribute: 'clip.length', type: 'range' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FiltersTitle
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findAllByProps({ variant: 'ICON' })[1]
        .props.onClick({ preventDefault: noop })

      component.root
        .findAllByProps({ variant: 'ICON' })[2]
        .props.onClick({ preventDefault: noop })
    })

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
      <FiltersTitle
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
