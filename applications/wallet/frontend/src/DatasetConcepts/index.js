import PropTypes from 'prop-types'

import Table from '../Table'

import DatasetConceptsRow from './Row'

const DatasetConcepts = ({ projectId, datasetId, actions }) => {
  return (
    <Table
      legend="Concepts"
      url={`/api/v1/projects/${projectId}/datasets/${datasetId}/get_labels/`}
      refreshKeys={[]}
      refreshButton={false}
      columns={
        actions
          ? ['Concept', 'Train Labels', 'Test Labels', '#Actions#']
          : ['Concept', 'Train Labels', 'Test Labels']
      }
      expandColumn={1}
      renderEmpty="There are currently no concepts for this dataset."
      renderRow={({ result, revalidate }) => (
        <DatasetConceptsRow
          key={result.label}
          projectId={projectId}
          datasetId={datasetId}
          actions={actions}
          concept={result}
          revalidate={revalidate}
        />
      )}
    />
  )
}

DatasetConcepts.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  actions: PropTypes.bool.isRequired,
}

export default DatasetConcepts
