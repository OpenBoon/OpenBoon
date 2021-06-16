import PropTypes from 'prop-types'

import DatasetConceptsMenu from './Menu'

const DatasetConceptsRow = ({
  projectId,
  datasetId,
  actions,
  concept: { label, trainCount, testCount },
  revalidate,
}) => {
  return (
    <tr>
      <td>{label}</td>

      <td>{trainCount}</td>

      <td>{testCount}</td>

      {actions && (
        <td>
          <DatasetConceptsMenu
            projectId={projectId}
            datasetId={datasetId}
            label={label}
            revalidate={revalidate}
          />
        </td>
      )}
    </tr>
  )
}

DatasetConceptsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  actions: PropTypes.bool.isRequired,
  concept: PropTypes.shape({
    label: PropTypes.string.isRequired,
    trainCount: PropTypes.number.isRequired,
    testCount: PropTypes.number.isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DatasetConceptsRow
