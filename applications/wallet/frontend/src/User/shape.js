import PropTypes from 'prop-types'

const userShape = {
  id: PropTypes.number.isRequired,
  username: PropTypes.string.isRequired,
  email: PropTypes.string.isRequired,
  firstName: PropTypes.string.isRequired,
  lastName: PropTypes.string.isRequired,
  organizations: PropTypes.arrayOf(PropTypes.string).isRequired,
}

export default userShape
