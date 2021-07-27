import PropTypes from 'prop-types'

import Pills from '../Pills'

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
        <Pills>
          {permissions.sort((a, b) => {
            if (a.toLowerCase() < b.toLowerCase()) return -1
            return 1
          })}
        </Pills>
      </td>
      <td>
        <ApiKeysMenu
          projectId={projectId}
          apiKeyId={apiKeyId}
          name={name}
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
