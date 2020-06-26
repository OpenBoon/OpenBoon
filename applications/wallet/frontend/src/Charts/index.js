import PropTypes from 'prop-types'
import { Responsive, WidthProvider } from 'react-grid-layout'

import chartShape from '../Chart/shape'

import ChartFacet from '../ChartFacet'
import ChartRange from '../ChartRange'

const ResponsiveGridLayout = WidthProvider(Responsive)

const BREAKPOINTS = ['sm', 'md', 'lg']
const MIN_COL_WIDTH = 350

const breakpoints = BREAKPOINTS.reduce(
  (acc, bp, index) => ({ ...acc, [bp]: MIN_COL_WIDTH * (index + 1) }),
  {},
)

const cols = BREAKPOINTS.reduce(
  (acc, bp, index) => ({ ...acc, [bp]: index + 1 }),
  {},
)

const Charts = ({ charts, dispatch }) => {
  const layouts = charts.map(({ id }, index) => ({
    i: id,
    x: index,
    y: 0,
    w: 1,
    h: 2,
  }))

  return (
    <ResponsiveGridLayout
      layouts={{ lg: layouts }}
      breakpoints={breakpoints}
      cols={cols}
    >
      {charts
        .filter(({ id }) => !!id)
        .map((chart, index) => {
          switch (chart.type) {
            case 'facet': {
              return (
                <div key={chart.id}>
                  <ChartFacet
                    chart={chart}
                    chartIndex={index}
                    dispatch={dispatch}
                  />
                </div>
              )
            }

            case 'range': {
              return (
                <div key={chart.id}>
                  <ChartRange
                    chart={chart}
                    chartIndex={index}
                    dispatch={dispatch}
                  />
                </div>
              )
            }

            default:
              return null
          }
        })}
    </ResponsiveGridLayout>
  )
}

Charts.propTypes = {
  charts: PropTypes.arrayOf(PropTypes.shape(chartShape).isRequired).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default Charts
