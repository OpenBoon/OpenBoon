import PropTypes from 'prop-types'

const OrganizationUsersRow = ({
  user: { email, firstName, lastName, projectCount },
}) => {
  return (
    <tr>
      <td>{email}</td>
      <td>{firstName}</td>
      <td>{lastName}</td>
      <td>{projectCount}</td>
      <td />
    </tr>
  )
}

OrganizationUsersRow.propTypes = {
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    email: PropTypes.string.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
    projectCount: PropTypes.number.isRequired,
  }).isRequired,
}

export default OrganizationUsersRow
