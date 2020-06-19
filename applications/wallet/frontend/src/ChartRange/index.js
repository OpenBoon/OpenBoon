import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

const ChartRange = ({ chart }) => {
  return <div>{JSON.stringify(chart)}</div>
}

ChartRange.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartRange
