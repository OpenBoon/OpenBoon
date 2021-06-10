import { useRouter } from 'next/router'

import Table from '../Table'

import DatasetConceptsRow from './Row'

const DatasetConcepts = () => {
  const {
    query: { projectId, datasetId },
  } = useRouter()

  return (
    <Table
      legend="Concepts"
      url={`/api/v1/projects/${projectId}/datasets/${datasetId}/get_labels/`}
      refreshKeys={[]}
      refreshButton={false}
      columns={['Concept', 'Train Labels', 'Test Labels', '#Actions#']}
      expandColumn={1}
      renderEmpty="There are currently no concepts for this dataset."
      renderRow={({ result, revalidate }) => (
        <DatasetConceptsRow
          key={result.label}
          projectId={projectId}
          datasetId={datasetId}
          concept={result}
          revalidate={revalidate}
        />
      )}
    />
  )
}

export default DatasetConcepts
