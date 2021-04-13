import { useContext } from 'react'
import PropTypes from 'prop-types'

import { UserContext } from '../User'

import Pills from '../Pills'

import ProjectUsersMenu from './Menu'

const ProjectUsersRow = ({
  projectId,
  user: { id: userId, email, roles },
  revalidate,
}) => {
  const {
    user: { email: currentUserEmail },
  } = useContext(UserContext)

  return (
    <tr>
      <td>{email}</td>
      <td>
        <Pills>{roles}</Pills>
      </td>
      <td>
        {email !== currentUserEmail &&
          !roles.includes('Organization_Owner') && (
            <ProjectUsersMenu
              projectId={projectId}
              userId={userId}
              revalidate={revalidate}
            />
          )}
      </td>
    </tr>
  )
}

ProjectUsersRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    email: PropTypes.string.isRequired,
    roles: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ProjectUsersRow
