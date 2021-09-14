import PropTypes from 'prop-types'

const filterShape = {
  type: PropTypes.oneOf([
    'exists',
    'labelsExist',
    'range',
    'predictionCount',
    'facet',
    'labelConfidence',
    'textContent',
    'similarity',
    'date',
    'label',
    'limit',
    'simpleSort',
  ]),
  attribute: PropTypes.string.isRequired,
  values: PropTypes.shape({}),
  isDisabled: PropTypes.bool,
}

export default filterShape
