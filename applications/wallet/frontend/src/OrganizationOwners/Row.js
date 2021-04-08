import { useContext } from 'react'
import PropTypes from 'prop-types'

import { UserContext } from '../User'

import OrganizationOwnersMenu from './Menu'

const OrganizationOwnersRow = ({
  organizationId,
  owner: { id, email, firstName, lastName },
  revalidate,
}) => {
  const {
    user: { email: currentUserEmail },
  } = useContext(UserContext)

  return (
    <tr>
      <td>{email}</td>
      <td>{firstName}</td>
      <td>{lastName}</td>
      <td>
        {email !== currentUserEmail && (
          <OrganizationOwnersMenu
            organizationId={organizationId}
            ownerId={id}
            revalidate={revalidate}
          />
        )}
      </td>
    </tr>
  )
}

OrganizationOwnersRow.propTypes = {
  organizationId: PropTypes.string.isRequired,
  owner: PropTypes.shape({
    id: PropTypes.number.isRequired,
    email: PropTypes.string.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationOwnersRow
