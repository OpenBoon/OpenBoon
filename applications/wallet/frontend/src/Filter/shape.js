import PropTypes from 'prop-types'

const filterShape = {
  type: PropTypes.oneOf([
    'exists',
    'range',
    'facet',
    'labelConfidence',
    'textContent',
    'similarity',
  ]).isRequired,
  attribute: PropTypes.string.isRequired,
  values: PropTypes.shape({}),
}

export default filterShape
