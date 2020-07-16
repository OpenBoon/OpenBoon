import PropTypes from 'prop-types'
import Router from 'next/router'
import Link from 'next/link'

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
      onClick={(event) => {
        const { target: { localName } = {} } = event || {}
        if (['a', 'button', 'svg', 'path'].includes(localName)) return
        Router.push(
          '/[projectId]/models/[modelId]',
          `/${projectId}/models/${modelId}`,
        )
      }}
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

      <td>{trained}</td>

      <td>{applied}</td>
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
