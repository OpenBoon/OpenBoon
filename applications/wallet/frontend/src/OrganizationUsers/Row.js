import PropTypes from 'prop-types'
import Link from 'next/link'

import { onRowClickRouterPush } from '../Table/helpers'

import OrganizationUsersMenu from './Menu'

const OrganizationUsersRow = ({
  organizationId,
  user,
  user: { id: userId, email, firstName, lastName, projectCount },
  revalidate,
}) => {
  return (
    <tr
      css={{ cursor: 'pointer' }}
      onClick={onRowClickRouterPush(
        '/organizations/[organizationId]/users/[userId]',
        `/organizations/${organizationId}/users/${userId}`,
      )}
    >
      <td>
        <Link
          href="/organizations/[organizationId]/users/[userId]"
          as={`/organizations/${organizationId}/users/${userId}`}
          passHref
        >
          <a css={{ ':hover': { textDecoration: 'none' } }}>{email}</a>
        </Link>
      </td>
      <td>{firstName}</td>
      <td>{lastName}</td>
      <td>{projectCount}</td>
      <td>
        <OrganizationUsersMenu
          organizationId={organizationId}
          user={user}
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
