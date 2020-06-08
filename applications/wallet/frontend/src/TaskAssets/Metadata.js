import PropTypes from 'prop-types'
import useSWR from 'swr'

import JsonDisplay from '../JsonDisplay'

const TaskAssetsMetadata = ({ projectId, assetId }) => {
  const {
    data: { metadata },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  return <JsonDisplay json={metadata} />
}

TaskAssetsMetadata.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default TaskAssetsMetadata
