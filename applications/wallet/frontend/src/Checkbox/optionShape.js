import PropTypes from 'prop-types'

const optionShape = {
  value: PropTypes.string.isRequired,
  label: PropTypes.node.isRequired,
  icon: PropTypes.node,
  legend: PropTypes.string,
  initialValue: PropTypes.bool.isRequired,
  isDisabled: PropTypes.bool.isRequired,
  supportedMedia: PropTypes.arrayOf(
    PropTypes.oneOf(['Images', 'Documents', 'Videos']).isRequired,
  ),
}

export default optionShape
