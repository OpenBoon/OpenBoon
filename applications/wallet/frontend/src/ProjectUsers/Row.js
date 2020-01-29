import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import ProjectUsersMenu from './Menu'

const ProjectUsersRow = ({
  projectId,
  user: { id: userId, name, email, permissions },
  revalidate,
}) => {
  return (
    <tr>
      <td>{name}</td>
      <td>{email}</td>
      <td>
        {permissions.map(permission => (
          <span
            key={permission}
            css={{
              display: 'inline-block',
              color: colors.structure.coal,
              backgroundColor: colors.structure.zinc,
              padding: spacing.moderate,
              paddingTop: spacing.small,
              paddingBottom: spacing.small,
              marginRight: spacing.base,
              borderRadius: constants.borderRadius.large,
            }}>
            {permission.replace(/([A-Z])/g, match => ` ${match}`)}
          </span>
        ))}
      </td>
      <td>
        <ProjectUsersMenu
          projectId={projectId}
          userId={userId}
          revalidate={revalidate}
        />
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
