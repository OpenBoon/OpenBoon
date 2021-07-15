import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import ModelLink from '../ModelLink'
import DatasetConcepts from '../DatasetConcepts'

import ModelDatasetHeader from './Header'

const ModelDataset = ({ setErrors }) => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
  )

  if (!model.datasetId) {
    return <ModelLink />
  }

  return (
    <div css={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
      <ModelDatasetHeader
        projectId={projectId}
        modelId={modelId}
        model={model}
        setErrors={setErrors}
      />

      <DatasetConcepts
        projectId={projectId}
        datasetId={model.datasetId}
        actions={false}
      />
    </div>
  )
}

ModelDataset.propTypes = {
  setErrors: PropTypes.func.isRequired,
}

export default ModelDataset
