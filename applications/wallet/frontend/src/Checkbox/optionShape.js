import PropTypes from 'prop-types'

const optionShape = {
  value: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  initialValue: PropTypes.bool.isRequired,
  isDisabled: PropTypes.bool.isRequired,
}

export default optionShape
