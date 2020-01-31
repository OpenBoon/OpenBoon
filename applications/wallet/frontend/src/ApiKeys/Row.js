import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import ApiKeysMenu from './Menu'

const ApiKeysRow = ({
  projectId,
  apiKey: { id: apiKeyId, name, permissions },
  revalidate,
}) => {
  return (
    <tr>
      <td>{name}</td>
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
        <ApiKeysMenu
          projectId={projectId}
          apiKeyId={apiKeyId}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

ApiKeysRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  apiKey: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    permissions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ApiKeysRow
