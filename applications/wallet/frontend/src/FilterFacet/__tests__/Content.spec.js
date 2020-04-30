import TestRenderer from 'react-test-renderer'

import facetAggregate from '../__mocks__/aggregate'

import FilterFacetContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<FilterFacetContent />', () => {
  it('should render selected', () => {
    const filters = {
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
        filters={[filters]}
        filter={filters}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render with no docCount', () => {
    const filters = { attribute: 'location.city', type: 'facet', values: {} }

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
        filters={[filters]}
        filter={filters}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
