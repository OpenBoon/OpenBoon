import PropTypes from 'prop-types'

import Pills from '../Pills'

import OrganizationUserProjectsMenu from './Menu'

const OrganizationUserProjectsRow = ({
  userId,
  project: { id: projectId, name, roles },
  revalidate,
}) => {
  return (
    <tr>
      <td>{name}</td>

      <td>
        <Pills>{roles}</Pills>
      </td>

      <td>
        <OrganizationUserProjectsMenu
          userId={userId}
          projectId={projectId}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

OrganizationUserProjectsRow.propTypes = {
  userId: PropTypes.number.isRequired,
  project: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    roles: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationUserProjectsRow
