import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

const ChartHistogramContent = ({
  chart: { type, id, attribute, scale, values },
}) => {
  return (
    <>
      <div>{`type: ${type}`}</div>
      <div>{`id: ${id}`}</div>
      <div>{`attribute: ${attribute}`}</div>
      <div>{`scale: ${scale}`}</div>
      <div>{`values: ${values}`}</div>
    </>
  )
}

ChartHistogramContent.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartHistogramContent
