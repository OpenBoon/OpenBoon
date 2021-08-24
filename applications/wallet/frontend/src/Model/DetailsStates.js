import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import SectionTitle from '../SectionTitle'
import ModelUpload from '../ModelUpload'
import ModelsEdit from '../ModelsEdit'

import ModelTrain from './Train'

const DEPLOY_ERROR = 'DeployError'
const REQUIRES_UPLOAD = ['RequiresUpload', DEPLOY_ERROR]
const DEPLOYING = 'Deploying'

const ModelDetailsStates = ({ projectId, model, modelTypes, setError }) => {
  const { pathname } = useRouter()

  const { type, state } = model

  const { label } = modelTypes.find(({ name: n }) => n === type) || {
    label: type,
  }

  if (pathname === '/[projectId]/models/[modelId]/edit') {
    return <ModelsEdit projectId={projectId} model={model} />
  }

  if (state === DEPLOYING) {
    return (
      <div css={{ display: 'flex', paddingTop: spacing.normal }}>
        <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
          Your {label} file was uploaded and is being processed.
        </FlashMessage>
      </div>
    )
  }

  if (REQUIRES_UPLOAD.includes(state)) {
    return (
      <div>
        {state === DEPLOY_ERROR && (
          <div css={{ display: 'flex', paddingTop: spacing.normal }}>
            <FlashMessage variant={FLASH_VARIANTS.ERROR}>
              Previously uploaded model deployment failed. Please try again or
              contact support for help.
            </FlashMessage>
          </div>
        )}

        <SectionTitle>Upload {label} File</SectionTitle>

        <div css={{ height: spacing.normal }} />

        <ModelUpload />
      </div>
    )
  }

  return <ModelTrain projectId={projectId} model={model} setError={setError} />
}

ModelDetailsStates.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    datasetId: PropTypes.string,
    runningJobId: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
  }).isRequired,
  modelTypes: PropTypes.arrayOf(PropTypes.shape().isRequired).isRequired,
  setError: PropTypes.func.isRequired,
}

export default ModelDetailsStates
