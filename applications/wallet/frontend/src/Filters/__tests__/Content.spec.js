import TestRenderer from 'react-test-renderer'

import facetAggregate from '../../FilterFacet/__mocks__/aggregate'
import rangeAggregate from '../../FilterRange/__mocks__/aggregate'
import labelConfidenceAggregate from '../../FilterLabelConfidence/__mocks__/aggregate'

import FiltersContent from '../Content'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filter/Reset', () => 'FilterReset')

describe('<FiltersContent />', () => {
  it('should render the "Exists" filter', () => {
    const filters = [{ type: 'exists', attribute: 'location.point' }]

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render the "Facet" filter', () => {
    const filters = [{ type: 'facet', attribute: 'location.city', values: {} }]

    require('swr').__setMockUseSWRResponse({ data: facetAggregate })

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render the "Range" filter', () => {
    const filters = [{ type: 'range', attribute: 'clip.length', values: {} }]

    require('swr').__setMockUseSWRResponse({
      data: rangeAggregate,
    })

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render the "Label Confidence" filter', () => {
    const filters = [
      {
        type: 'labelConfidence',
        attribute: 'analysis.zvi-label-detection',
        values: {},
      },
    ]

    require('swr').__setMockUseSWRResponse({ data: labelConfidenceAggregate })

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render the "Text Detection" filter', () => {
    const filters = [
      {
        type: 'textContent',
        attribute: 'analysis.zvi-text-content',
        values: {},
      },
    ]

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render the default filter', () => {
    const mockRouterPush = jest.fn()
    require('next/router').__setMockPushFunction(mockRouterPush)

    const filters = [
      {
        type: 'similarity',
        attribute: '',
        values: {},
      },
    ]

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
