import { useRouter } from 'next/router'
import useSWR from 'swr'

import DatasetModelsTable from './Table'

const DatasetModels = () => {
  const {
    query: { projectId, datasetId },
  } = useRouter()

  const {
    data: { results: modelTypes },
  } = useSWR(`/api/v1/projects/${projectId}/models/model_types/`)

  return (
    <DatasetModelsTable
      projectId={projectId}
      datasetId={datasetId}
      modelTypes={modelTypes}
    />
  )
}

export default DatasetModels
