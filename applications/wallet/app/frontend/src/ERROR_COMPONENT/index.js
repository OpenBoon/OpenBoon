import PropTypes from 'prop-types'

const ERROR_COMPONENT = ({ error }) => {
  return <div>{error}</div>
}

ERROR_COMPONENT.propTypes = {
  error: PropTypes.string.isRequired,
}

export default ERROR_COMPONENT
