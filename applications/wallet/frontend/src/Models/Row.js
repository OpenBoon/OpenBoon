import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, constants } from '../Styles'

import { onRowClickRouterPush } from '../Table/helpers'

import CheckmarkSvg from '../Icons/checkmark.svg'

const ModelsRow = ({
  projectId,
  model: { id: modelId, name, type, moduleName, ready },
}) => {
  return (
    <tr
      css={{ cursor: 'pointer' }}
      onClick={onRowClickRouterPush(
        '/[projectId]/models/[modelId]',
        `/${projectId}/models/${modelId}`,
      )}
    >
      <td>
        <Link
          href="/[projectId]/models/[modelId]"
          as={`/${projectId}/models/${modelId}`}
          passHref
        >
          <a css={{ ':hover': { textDecoration: 'none' } }}>{name}</a>
        </Link>
      </td>

      <td>{type}</td>

      <td>{moduleName}</td>

      <td css={{ textAlign: 'center' }}>
        <CheckmarkSvg
          height={constants.icons.regular}
          color={ready ? colors.key.one : colors.structure.transparent}
        />
      </td>
    </tr>
  )
}

ModelsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    moduleName: PropTypes.string.isRequired,
    ready: PropTypes.bool.isRequired,
  }).isRequired,
}

export default ModelsRow
