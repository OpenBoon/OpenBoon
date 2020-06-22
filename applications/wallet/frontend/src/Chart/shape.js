import PropTypes from 'prop-types'

const chartShape = {
  id: PropTypes.string.isRequired,
  type: PropTypes.oneOf(['FACET', 'RANGE']),
  attribute: PropTypes.string,
}

export default chartShape
