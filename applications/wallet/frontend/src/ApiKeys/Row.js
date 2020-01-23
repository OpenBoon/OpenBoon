import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

const ApiKeysRow = ({ apiKey: { name, permissions } }) => {
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
    </tr>
  )
}

ApiKeysRow.propTypes = {
  apiKey: PropTypes.shape({
    name: PropTypes.string.isRequired,
    permissions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  }).isRequired,
}

export default ApiKeysRow
