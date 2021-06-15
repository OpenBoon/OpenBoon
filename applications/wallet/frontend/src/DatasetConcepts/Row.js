import PropTypes from 'prop-types'

import DatasetConceptsMenu from './Menu'

const DatasetConceptsRow = ({
  projectId,
  datasetId,
  concept: { label, count },
  revalidate,
}) => {
  return (
    <tr>
      <td>{label}</td>

      <td>{count}</td>

      <td>{count}</td>

      <td>
        <DatasetConceptsMenu
          projectId={projectId}
          datasetId={datasetId}
          label={label}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

DatasetConceptsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  concept: PropTypes.shape({
    label: PropTypes.string.isRequired,
    count: PropTypes.number.isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DatasetConceptsRow
