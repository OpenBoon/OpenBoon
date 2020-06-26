import PropTypes from 'prop-types'
import GridLayout from 'react-grid-layout'

import chartShape from '../Chart/shape'

import ChartFacet from '../ChartFacet'
import ChartRange from '../ChartRange'

const Charts = ({ charts, dispatch }) => {
  const layout = charts.map(({ id }, index) => ({
    i: id,
    x: 0,
    y: index,
    w: 1,
    h: 2,
  }))

  // console.warn({ layout })

  return (
    <GridLayout
      className="layout"
      layout={layout}
      width={798}
      rowHeight={50}
      cols={6}
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
    </GridLayout>
  )
}

Charts.propTypes = {
  charts: PropTypes.arrayOf(PropTypes.shape(chartShape).isRequired).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default Charts
