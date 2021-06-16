import { useRouter } from 'next/router'
import useSWR from 'swr'

import ModelDetails from './Details'

const ModelContent = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const {
    data: { results: modelTypes },
  } = useSWR(`/api/v1/projects/${projectId}/models/model_types/`)

  return (
    <ModelDetails
      projectId={projectId}
      modelId={modelId}
      modelTypes={modelTypes}
    />
  )
}

export default ModelContent
