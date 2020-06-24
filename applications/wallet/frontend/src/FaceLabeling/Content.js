import PropTypes from 'prop-types'
import useSWR from 'swr'

import SuspenseBoundary from '../SuspenseBoundary'
import { colors, spacing } from '../Styles'

import FaceLabelingLabels from './Labels'

const FaceLabelingContent = ({ projectId, assetId }) => {
  const { data } = useSWR(`/api/v1/projects/${projectId}/faces/${assetId}/`)

  const { predictions = [] } = data || {}

  if (predictions.length < 1) {
    return (
      <div css={{ padding: spacing.normal, color: colors.structure.white }}>
        No faces have been detected in this asset for naming and training.
        Please select another asset.
      </div>
    )
  }

  return (
    <SuspenseBoundary>
      <FaceLabelingLabels
        projectId={projectId}
        assetId={assetId}
        predictions={predictions}
      />
    </SuspenseBoundary>
  )
}

FaceLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default FaceLabelingContent
