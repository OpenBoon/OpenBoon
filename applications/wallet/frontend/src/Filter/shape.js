import PropTypes from 'prop-types'

const filterShape = {
  type: PropTypes.oneOf([
    'search',
    'facet',
    'range',
    'exists',
    'labelConfidence',
  ]).isRequired,
  attribute: PropTypes.string.isRequired,
  values: PropTypes.shape({}),
}

export default filterShape
