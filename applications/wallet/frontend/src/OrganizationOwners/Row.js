import PropTypes from 'prop-types'

const OrganizationOwnersRow = ({ owner: { email, firstName, lastName } }) => {
  return (
    <tr>
      <td>{email}</td>
      <td>{firstName}</td>
      <td>{lastName}</td>
      <td />
    </tr>
  )
}

OrganizationOwnersRow.propTypes = {
  owner: PropTypes.shape({
    id: PropTypes.number.isRequired,
    email: PropTypes.string.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
  }).isRequired,
}

export default OrganizationOwnersRow
