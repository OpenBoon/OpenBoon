import TestRenderer from 'react-test-renderer'

import facetAggregate from '../__mocks__/aggregate'

import FilterFacetContent, { noop } from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filters/Reset', () => 'FiltersReset')

describe('<FilterFacetContent />', () => {
  it('should render selected', () => {
    const filter = {
      attribute: 'location.city',
      type: 'facet',
      values: { Tyngsboro: true },
    }

    require('swr').__setMockUseSWRResponse({
      data: {
        ...facetAggregate,
        results: {
          ...facetAggregate.results,
          buckets: [
            { key: 'Tyngsboro' },
            { key: 'Brooklyn' },
            { key: 'Cát Bà' },
          ],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterFacetContent
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render with no docCount', () => {
    const filter = { attribute: 'location.city', type: 'facet', values: {} }

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 596,
        results: {
          docCountErrorUpperBound: 0,
          sumOtherDocCount: 0,
          buckets: [],
        },
      },
    })

    const component = TestRenderer.create(
      <FilterFacetContent
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
