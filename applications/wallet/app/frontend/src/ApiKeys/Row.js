import PropTypes from 'prop-types'

const ApiKeysRow = ({ apiKey: { name, permissions } }) => {
  return (
    <tr>
      <td>{name}</td>
      <td>
        {permissions.join(', ').replace(/([A-Z])/g, match => ` ${match}`)}
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
