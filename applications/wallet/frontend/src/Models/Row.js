import PropTypes from 'prop-types'
import Link from 'next/link'

import { onRowClickRouterPush } from '../Table/helpers'

import ModelsMenu from './Menu'

const ModelsRow = ({
  projectId,
  model: { id: modelId, name, type, moduleName },
  modelTypes,
}) => {
  const { label } = modelTypes.find(({ name: n }) => n === type) || {
    label: type,
  }

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

      <td>{label}</td>

      <td>{moduleName}</td>

      <td>
        <ModelsMenu projectId={projectId} modelId={modelId} />
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
  }).isRequired,
  modelTypes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default ModelsRow
