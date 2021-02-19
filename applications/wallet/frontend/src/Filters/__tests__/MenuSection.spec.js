import TestRenderer from 'react-test-renderer'

import models from '../../Models/__mocks__/models'
import fields from '../__mocks__/fields'

import FiltersMenuSection from '../MenuSection'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = models.results[0].id

const noop = () => () => {}

describe('<FiltersMenuSection />', () => {
  it('should render properly if a field has no possible filter', () => {
    const component = TestRenderer.create(
      <FiltersMenuSection
        projectId={PROJECT_ID}
        path="location"
        attribute="point"
        value={[]}
        filters={[]}
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for an "exists" filter', () => {
    const component = TestRenderer.create(
      <FiltersMenuSection
        projectId={PROJECT_ID}
        path="location"
        attribute="point"
        value={['exists']}
        filters={[]}
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for a "label" filter', () => {
    require('swr').__setMockUseSWRResponse({ data: models })

    const component = TestRenderer.create(
      <FiltersMenuSection
        projectId={PROJECT_ID}
        path="labels"
        attribute={MODEL_ID}
        value={['label']}
        filters={[]}
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should not render with missing model', () => {
    require('swr').__setMockUseSWRResponse({ data: { results: [] } })

    const component = TestRenderer.create(
      <FiltersMenuSection
        projectId={PROJECT_ID}
        path="labels"
        attribute={MODEL_ID}
        value={['label']}
        filters={[]}
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toEqual(null)
  })

  it('should render properly for a sub-section', () => {
    require('swr').__setMockUseSWRResponse({ data: models })

    const component = TestRenderer.create(
      <FiltersMenuSection
        projectId={PROJECT_ID}
        path="analysis"
        attribute="boonai"
        value={fields.analysis.boonai}
        filters={[]}
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
