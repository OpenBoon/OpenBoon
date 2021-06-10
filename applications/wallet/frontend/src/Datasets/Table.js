import PropTypes from 'prop-types'

import Table, { ROLES } from '../Table'

import DatasetsRow from './Row'

const DatasetsTable = ({ projectId, datasetTypes }) => {
  return (
    <Table
      role={ROLES.ML_Tools}
      legend="Datasets"
      url={`/api/v1/projects/${projectId}/datasets/`}
      refreshKeys={[]}
      refreshButton={false}
      columns={['Name', 'Type', 'Linked Models', '#Actions#']}
      expandColumn={0}
      renderEmpty="There are currently no datasets."
      renderRow={({ result, revalidate }) => (
        <DatasetsRow
          key={result.id}
          projectId={projectId}
          dataset={result}
          datasetTypes={datasetTypes}
          revalidate={revalidate}
        />
      )}
    />
  )
}

DatasetsTable.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetTypes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      description: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default DatasetsTable
