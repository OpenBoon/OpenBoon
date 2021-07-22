import { useContext } from 'react'
import PropTypes from 'prop-types'

import { UserContext } from '../User'

import Pills from '../Pills'

import ProjectUsersMenu from './Menu'

const ProjectUsersRow = ({ projectId, user, revalidate }) => {
  const {
    user: { email: currentUserEmail },
  } = useContext(UserContext)

  return (
    <tr>
      <td>{user.email}</td>
      <td>{user.first_name}</td>
      <td>{user.last_name}</td>
      <td>
        <Pills>{user.roles}</Pills>
      </td>
      <td>
        {user.email !== currentUserEmail &&
          !user.roles.includes('Organization_Owner') && (
            <ProjectUsersMenu
              projectId={projectId}
              user={user}
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
    first_name: PropTypes.string.isRequired,
    last_name: PropTypes.string.isRequired,
    roles: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ProjectUsersRow
