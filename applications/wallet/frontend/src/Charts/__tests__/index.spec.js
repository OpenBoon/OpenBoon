import TestRenderer from 'react-test-renderer'

import Charts from '..'

const CHART_ID = '972a8ab5-cdcb-4eea-ada7-f1c88d997fed'

describe('<Charts />', () => {
  it('should render properly for "Range"', () => {
    const component = TestRenderer.create(
      <Charts charts={[{ id: CHART_ID, type: 'RANGE' }]} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for "Facet"', () => {
    const component = TestRenderer.create(
      <Charts charts={[{ id: CHART_ID, type: 'FACET' }]} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly for "Unknown"', () => {
    const component = TestRenderer.create(
      <Charts charts={[{ id: CHART_ID }]} />,
    )

    expect(component.toJSON()).toEqual(null)
  })
})
