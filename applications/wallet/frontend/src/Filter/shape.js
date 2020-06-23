import PropTypes from 'prop-types'

const filterShape = {
  type: PropTypes.oneOf([
    'exists',
    'range',
    'facet',
    'labelConfidence',
    'textContent',
    'similarity',
  ]),
  attribute: PropTypes.string.isRequired,
  values: PropTypes.shape({}),
  isDisabled: PropTypes.bool,
}

export default filterShape
