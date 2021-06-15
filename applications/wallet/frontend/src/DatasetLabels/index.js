import { useRouter } from 'next/router'
import useSWR from 'swr'

import DatasetLabelsContent from './Content'

const DatasetLabels = () => {
  const {
    query: { projectId, datasetId, query = '', page = 1 },
  } = useRouter()

  const {
    data: { name },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/${datasetId}/`)

  return (
    <DatasetLabelsContent
      projectId={projectId}
      datasetId={datasetId}
      query={query}
      page={parseInt(page, 10)}
      datasetName={name}
    />
  )
}

export default DatasetLabels
