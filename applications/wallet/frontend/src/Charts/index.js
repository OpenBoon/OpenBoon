import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

import ChartFacet from '../ChartFacet'
import ChartRange from '../ChartRange'

const Charts = ({ charts }) => {
  return charts
    .filter(({ id }) => !!id)
    .map((chart) => {
      switch (chart.type) {
        case 'FACET': {
          return <ChartFacet key={chart.id} chart={chart} />
        }

        case 'RANGE': {
          return <ChartRange key={chart.id} chart={chart} />
        }

        default:
          return null
      }
    })
}

Charts.propTypes = {
  charts: PropTypes.arrayOf(PropTypes.shape(chartShape).isRequired).isRequired,
}

export default Charts
