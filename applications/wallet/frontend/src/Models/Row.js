import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors } from '../Styles'

import { onRowClickRouterPush } from '../Table/helpers'

import CheckmarkSvg from '../Icons/checkmark.svg'

const ICON_SIZE = 20

const ModelsRow = ({
  projectId,
  model: {
    id: modelId,
    modelName,
    modelType,
    moduleName,
    labelsCount,
    assetsCount,
    trained,
    applied,
  },
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
          <a css={{ ':hover': { textDecoration: 'none' } }}>{modelName}</a>
        </Link>
      </td>

      <td>{modelType}</td>

      <td>{moduleName}</td>

      <td>{labelsCount}</td>

      <td>{assetsCount}</td>

      <td css={{ textAlign: 'center' }}>
        {!!trained && (
          <CheckmarkSvg height={ICON_SIZE} color={colors.key.one} />
        )}
      </td>

      <td css={{ textAlign: 'center' }}>
        {!!applied && (
          <CheckmarkSvg height={ICON_SIZE} color={colors.key.one} />
        )}
      </td>
    </tr>
  )
}

ModelsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    modelName: PropTypes.string.isRequired,
    modelType: PropTypes.string.isRequired,
    moduleName: PropTypes.string.isRequired,
    labelsCount: PropTypes.number.isRequired,
    assetsCount: PropTypes.number.isRequired,
    trained: PropTypes.bool.isRequired,
    applied: PropTypes.bool.isRequired,
  }).isRequired,
}

export default ModelsRow
