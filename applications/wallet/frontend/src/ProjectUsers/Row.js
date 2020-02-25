import { useContext } from 'react'
import PropTypes from 'prop-types'

import { UserContext } from '../User'

import Pills from '../Pills'

import ProjectUsersMenu from './Menu'

const ProjectUsersRow = ({
  projectId,
  user: { id: userId, email, permissions },
  revalidate,
}) => {
  const {
    user: { email: currentUserEmail },
  } = useContext(UserContext)

  return (
    <tr>
      <td>{email}</td>
      <td>
        <Pills>{permissions}</Pills>
      </td>
      <td>
        {email !== currentUserEmail && (
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
    permissions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ProjectUsersRow
