import PropTypes from 'prop-types'

import Table, { ROLES } from '../Table'

import DatasetModelsRow from './Row'

const DatasetModelsTable = ({ projectId, datasetId, modelTypes }) => {
  return (
    <Table
      role={ROLES.ML_Tools}
      legend="Models"
      url={`/api/v1/projects/${projectId}/datasets/${datasetId}/get_models/`}
      refreshKeys={[]}
      refreshButton={false}
      columns={['Name', 'Type', '#Actions#']}
      expandColumn={0}
      renderEmpty="There are currently no models associated with this dataset."
      renderRow={({ result, revalidate }) => (
        <DatasetModelsRow
          key={result.id}
          projectId={projectId}
          model={result}
          modelTypes={modelTypes}
          revalidate={revalidate}
        />
      )}
    />
  )
}

DatasetModelsTable.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  modelTypes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default DatasetModelsTable
