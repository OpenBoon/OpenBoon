import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import FaceLabelingForm from './Form'
import FaceLabelingTrainApply from './TrainApply'

const FaceLabelingLabels = ({ projectId, assetId, predictions }) => {
  const { data: asset } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/`,
  )

  const {
    metadata: {
      source: { filename },
    },
  } = asset

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

FaceLabelingLabels.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  predictions: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
}

export default FaceLabelingLabels
