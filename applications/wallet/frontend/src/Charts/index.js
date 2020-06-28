import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

import ChartFacet from '../ChartFacet'
import ChartRange from '../ChartRange'

const Charts = ({ charts, dispatch }) => {
  return charts
    .filter(({ id }) => !!id)
    .map((chart, index) => {
      switch (chart.type) {
        case 'facet': {
          return (
            <ChartFacet
              key={chart.id}
              chart={chart}
              chartIndex={index}
              dispatch={dispatch}
            />
          )
        }

        case 'range': {
          return (
            <ChartRange
              key={chart.id}
              chart={chart}
              chartIndex={index}
              dispatch={dispatch}
            />
          )
        }

        default:
          return null
      }
    })
}

Charts.propTypes = {
  charts: PropTypes.arrayOf(PropTypes.shape(chartShape).isRequired).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default Charts
