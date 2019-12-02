import PropTypes from 'prop-types'

const Jobs = ({ logout }) => (
  <button type="button" onClick={logout}>
    Logout
  </button>
)

Jobs.propTypes = {
  logout: PropTypes.func.isRequired,
}

export default Jobs
