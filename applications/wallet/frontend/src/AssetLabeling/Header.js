import useSWR from 'swr'
import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

const AssetLabelingHeader = ({ projectId, assetId }) => {
  const {
    data: {
      metadata: {
        source: { filename },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  return (
    <div
      css={{
        padding: spacing.normal,
        borderBottom: constants.borders.regular.smoke,
        color: colors.signal.sky.base,
      }}
    >
      {filename}
    </div>
  )
}

AssetLabelingHeader.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default AssetLabelingHeader
