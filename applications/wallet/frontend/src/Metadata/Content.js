import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import JsonDisplay from '../JsonDisplay'

const MetadataContent = ({ projectId, assetId }) => {
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
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            color: colors.signal.sky.base,
          }}
        >
          {filename}
        </div>
      </div>
      <div
        css={{
          height: '100%',
          overflow: 'auto',
          backgroundColor: colors.structure.coal,
          padding: spacing.normal,
        }}
      >
        <JsonDisplay json={asset} />
      </div>
    </>
  )
}

MetadataContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default MetadataContent
