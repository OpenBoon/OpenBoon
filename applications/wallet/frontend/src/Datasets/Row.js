import PropTypes from 'prop-types'
import Link from 'next/link'

import { onRowClickRouterPush } from '../Table/helpers'

import DatasetsMenu from './Menu'

const DatasetsRow = ({
  projectId,
  dataset: { id: datasetId, name, type, modelCount },
  datasetTypes,
  revalidate,
}) => {
  const { label } = datasetTypes.find(({ name: n }) => n === type) || {
    label: type,
  }

  return (
    <tr
      css={{ cursor: 'pointer' }}
      onClick={onRowClickRouterPush(
        '/[projectId]/datasets/[datasetId]',
        `/${projectId}/datasets/${datasetId}`,
      )}
    >
      <td>
        <Link
          href="/[projectId]/datasets/[datasetId]"
          as={`/${projectId}/datasets/${datasetId}`}
          passHref
        >
          <a css={{ ':hover': { textDecoration: 'none' } }}>{name}</a>
        </Link>
      </td>

      <td>{label}</td>

      <td>{modelCount}</td>

      <td>
        <DatasetsMenu
          projectId={projectId}
          datasetId={datasetId}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

DatasetsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  dataset: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    modelCount: PropTypes.number.isRequired,
  }).isRequired,
  datasetTypes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      description: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DatasetsRow
