import PropTypes from 'prop-types'

const optionShape = {
  value: PropTypes.string.isRequired,
  label: PropTypes.node.isRequired,
  icon: PropTypes.node,
  legend: PropTypes.string,
  initialValue: PropTypes.bool.isRequired,
  isDisabled: PropTypes.bool.isRequired,
}

export default optionShape
