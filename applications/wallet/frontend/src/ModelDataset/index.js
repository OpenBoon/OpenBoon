import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import ModelLink from '../ModelLink'
import DatasetConcepts from '../DatasetConcepts'

import ModelDatasetHeader from './Header'

const REQUIRES_UPLOAD = 'RequiresUpload'
const DEPLOYING = 'Deploying'
const DEPLOY_ERROR = 'DeployError'
const UPLOAD_STATES = [REQUIRES_UPLOAD, DEPLOYING, DEPLOY_ERROR]

const ModelDataset = ({ model, setErrors }) => {
  const {
    query: { projectId },
  } = useRouter()

  if (UPLOAD_STATES.includes(model.state)) {
    return null
  }

  if (!model.datasetId) {
    return <ModelLink model={model} />
  }

  return (
    <div css={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
      <ModelDatasetHeader
        projectId={projectId}
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
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    datasetId: PropTypes.string,
    runningJobId: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
  }).isRequired,
  setErrors: PropTypes.func.isRequired,
}

export default ModelDataset
