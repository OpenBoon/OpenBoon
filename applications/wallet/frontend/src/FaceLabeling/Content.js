import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import FaceLabelingForm from './Form'
import FaceLabelingTrainApply from './TrainApply'

const FaceLabelingContent = ({ projectId, assetId }) => {
  const { data } = useSWR(`/api/v1/projects/${projectId}/faces/${assetId}/`)

  const { filename, predictions = [] } = data || {}

  if (predictions.length < 1) {
    return (
      <div css={{ padding: spacing.normal, color: colors.structure.white }}>
        No faces have been detected in this asset for naming and training.
        Please select another asset.
      </div>
    )
  }

  return (
    <>
      <FaceLabelingTrainApply projectId={projectId} />

      <div
        css={{
          padding: spacing.normal,
          color: colors.signal.sky.base,
        }}
      >
        {filename}
      </div>

      <FaceLabelingForm
        projectId={projectId}
        assetId={assetId}
        predictions={predictions}
      />
    </>
  )
}

FaceLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default FaceLabelingContent
