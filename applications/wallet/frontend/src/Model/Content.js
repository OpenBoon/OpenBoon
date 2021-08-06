import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import ModelDataset from '../ModelDataset'
import ModelDeployment from '../ModelDeployment'

import ModelDetails from './Details'

const ModelContent = ({ setErrors }) => {
  const {
    pathname,
    query: { projectId, modelId, action },
  } = useRouter()

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
    { refreshInterval: 3000 },
  )

  return (
    <>
      <ModelDetails projectId={projectId} model={model} />

      {pathname === '/[projectId]/models/[modelId]' && (
        <ModelDataset
          key={`${model.datasetId}${action}`}
          model={model}
          setErrors={setErrors}
        />
      )}

      {pathname === '/[projectId]/models/[modelId]/deployment' && (
        <ModelDeployment />
      )}
    </>
  )
}

ModelContent.propTypes = {
  setErrors: PropTypes.func.isRequired,
}

export default ModelContent
