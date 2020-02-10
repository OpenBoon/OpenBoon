import PropTypes from 'prop-types'

import Pills from '../Pills'

import ProjectUsersMenu from './Menu'

import { getUser } from '../Authentication/helpers'

const ProjectUsersRow = ({
  projectId,
  user: { id: userId, email, permissions },
  revalidate,
}) => {
  const { email: currentUserEmail } = getUser()
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
    id: PropTypes.string.isRequired,
    email: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    permissions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ProjectUsersRow
