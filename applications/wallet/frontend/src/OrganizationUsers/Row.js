import PropTypes from 'prop-types'

import OrganizationUsersMenu from './Menu'

const OrganizationUsersRow = ({
  organizationId,
  user: { id, email, firstName, lastName, projectCount },
  revalidate,
}) => {
  return (
    <tr>
      <td>{email}</td>
      <td>{firstName}</td>
      <td>{lastName}</td>
      <td>{projectCount}</td>
      <td>
        <OrganizationUsersMenu
          organizationId={organizationId}
          userId={id}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

OrganizationUsersRow.propTypes = {
  organizationId: PropTypes.string.isRequired,
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    email: PropTypes.string.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
    projectCount: PropTypes.number.isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationUsersRow
