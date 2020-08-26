import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, spacing, typography } from '../Styles'

import FaceLabelingForm from './Form'

const FaceLabelingContent = ({ projectId, assetId }) => {
  const { data } = useSWR(`/api/v1/projects/${projectId}/faces/${assetId}/`)

  const { filename, predictions = [] } = data || {}

  if (predictions.length < 1) {
    return (
      <div
        css={{
          padding: spacing.normal,
          color: colors.structure.white,
          fontStyle: typography.style.italic,
        }}
      >
        No faces have been detected in this asset for naming and training.
        Please select another asset.
      </div>
    )
  }

  return (
    <>
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
