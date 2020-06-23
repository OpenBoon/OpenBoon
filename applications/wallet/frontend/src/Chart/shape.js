import PropTypes from 'prop-types'

const chartShape = {
  id: PropTypes.string.isRequired,
  type: PropTypes.oneOf(['facet', 'range']),
  attribute: PropTypes.string,
}

export default chartShape
