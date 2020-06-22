import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

const ChartFacet = ({ chart }) => {
  return <div>{JSON.stringify(chart)}</div>
}

ChartFacet.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartFacet
