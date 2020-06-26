import TestRenderer from 'react-test-renderer'

import Charts from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const CHART_ID = '972a8ab5-cdcb-4eea-ada7-f1c88d997fed'

jest.mock('../../ChartFacet', () => 'ChartFacet')
jest.mock('../../ChartRange', () => 'ChartRange')

const noop = () => () => {}

describe('<Charts />', () => {
  it('should render properly for "Range"', () => {
    const component = TestRenderer.create(
      <Charts
        projectId={PROJECT_ID}
        charts={[{ id: CHART_ID, type: 'range' }]}
        chartIndex={0}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for "Facet"', () => {
    const component = TestRenderer.create(
      <Charts
        projectId={PROJECT_ID}
        charts={[{ id: CHART_ID, type: 'facet' }]}
        chartIndex={0}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for "Unknown"', () => {
    const component = TestRenderer.create(
      <Charts
        projectId={PROJECT_ID}
        charts={[{ id: CHART_ID }]}
        chartIndex={0}
        dispatch={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
