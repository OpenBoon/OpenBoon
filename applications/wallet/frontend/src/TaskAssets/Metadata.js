import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import JsonDisplay from '../JsonDisplay'

const TaskAssetsMetadata = ({ projectId, assetId }) => {
  const {
    data: { metadata },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  return (
    <div
      css={{
        height: '100%',
        overflow: 'auto',
        backgroundColor: colors.structure.coal,
        pre: {
          padding: spacing.normal,
        },
      }}
    >
      <JsonDisplay json={metadata} />
    </div>
  )
}

TaskAssetsMetadata.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default TaskAssetsMetadata
