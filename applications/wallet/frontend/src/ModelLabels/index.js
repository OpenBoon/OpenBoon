import { useRouter } from 'next/router'

import Table from '../Table'

import ModelLabelsEmpty from './Empty'
import ModelLabelsRow from './Row'

const ModelLabels = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  return (
    <Table
      legend="Labels"
      url={`/api/v1/projects/${projectId}/models/${modelId}/get_labels/`}
      refreshKeys={[]}
      refreshButton={false}
      columns={['Label', 'Count', '#Actions#']}
      expandColumn={1}
      renderEmpty={<ModelLabelsEmpty />}
      renderRow={({ result, revalidate }) => (
        <ModelLabelsRow
          key={result.label}
          projectId={projectId}
          modelId={modelId}
          label={result}
          revalidate={revalidate}
        />
      )}
    />
  )
}

export default ModelLabels
