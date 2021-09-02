import TestRenderer, { act } from 'react-test-renderer'

import fields from '../../Filters/__mocks__/fields'

import SearchFilterSort from '../Sort'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'srL8ob5cTpCJjYoKkqqfa2ciyG425dGi'

describe('<SearchFilterSort />', () => {
  it('should render properly without a sort filter selected', () => {
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <SearchFilterSort
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={[]}
        fields={fields}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select attribute
    act(() => {
      component.root
        .findByProps({ label: 'Sort by' })
        .props.onChange({ value: 'location.city' })
    })

    let query = btoa(
      JSON.stringify([
        {
          type: 'simpleSort',
          attribute: 'location.city',
          values: { order: 'desc' },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )

    // Select sort direction
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Change sort direction' })
        .props.onClick()
    })

    query = btoa(
      JSON.stringify([
        {
          type: 'simpleSort',
          attribute: 'location.city',
          values: { order: 'desc' },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}&query=${query}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}&query=${query}`,
    )

    // Clear
    act(() => {
      component.root.findByProps({ 'aria-label': 'Clear sort' }).props.onClick()
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?assetId=${ASSET_ID}`,
      `/${PROJECT_ID}/visualizer?assetId=${ASSET_ID}`,
    )
  })

  it('should render properly with a sort filter selected', () => {
    const component = TestRenderer.create(
      <SearchFilterSort
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={[
          {
            type: 'simpleSort',
            attribute: 'location.city',
            values: { order: 'asc' },
          },
        ]}
        fields={fields}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Select sort direction
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Change sort direction' })
        .props.onClick()
    })
  })
})
