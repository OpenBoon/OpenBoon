import TestRenderer, { act } from 'react-test-renderer'

import aggregate from '../../FilterFacet/__mocks__/aggregate'

import { encode } from '../../Filters/helpers'

import FilterBuckets from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = ''

describe('<FilterBuckets />', () => {
  it('should select all properly', () => {
    const filter = {
      attribute: 'location.city',
      type: 'facet',
      values: { facets: [] },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterBuckets
        pathname="/[projectId]/visualizer"
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={[filter]}
        filter={filter}
        filterIndex={0}
        buckets={aggregate.results.buckets}
        searchString=""
      />,
    )

    act(() => {
      component.root
        .findByProps({ children: 'Select All' })
        .props.onClick({ preventDefault: noop })
    })

    const query = encode({
      filters: [
        {
          type: 'facet',
          attribute: 'location.city',
          values: {
            facets: [
              'Tyngsboro',
              'Brooklyn',
              'Cát Bà',
              'La Habana Vieja',
              'North Glendale',
              'Saalfelden am Steinernen Meer',
              'Zermatt',
            ],
          },
        },
      ],
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/[projectId]/visualizer?query=${query}`,
      `/${PROJECT_ID}/visualizer?query=${query}`,
    )
  })
})
