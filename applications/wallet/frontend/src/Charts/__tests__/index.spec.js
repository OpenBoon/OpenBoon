import TestRenderer from 'react-test-renderer'

import Charts from '..'

const CHART_ID = '972a8ab5-cdcb-4eea-ada7-f1c88d997fed'

jest.mock('../../ChartFacet', () => 'ChartFacet')
jest.mock('../../ChartRange', () => 'ChartRange')

const noop = () => () => {}

describe('<Charts />', () => {
  it('should render properly for "Range"', () => {
    const component = TestRenderer.create(
      <Charts
        charts={[{ id: CHART_ID, type: 'range' }]}
        layouts={{}}
        chartIndex={0}
        dispatch={noop}
        setLayouts={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for "Facet"', () => {
    const component = TestRenderer.create(
      <Charts
        charts={[{ id: CHART_ID, type: 'facet' }]}
        layouts={{}}
        chartIndex={0}
        dispatch={noop}
        setLayouts={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for "Histogram"', () => {
    const component = TestRenderer.create(
      <Charts
        charts={[{ id: CHART_ID, type: 'histogram' }]}
        layouts={{}}
        chartIndex={0}
        dispatch={noop}
        setLayouts={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for "Unknown"', () => {
    const component = TestRenderer.create(
      <Charts
        charts={[{ id: CHART_ID }]}
        layouts={{}}
        chartIndex={0}
        dispatch={noop}
        setLayouts={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
