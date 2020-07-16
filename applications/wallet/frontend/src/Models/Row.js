import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors } from '../Styles'

import { onRowClickRouterPush } from '../Table/helpers'

import CheckmarkSvg from '../Icons/checkmark.svg'

const ICON_SIZE = 20

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
        {!!ready && <CheckmarkSvg height={ICON_SIZE} color={colors.key.one} />}
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
