import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import MetadataPrettyPredictionsContent, {
  BBOX_SIZE,
} from './PredictionsContent'

const MetadataPrettyPredictionsQuery = ({ name, path }) => {
  const attr = `${path}.${name}&width=${BBOX_SIZE}`

  const {
    query: { projectId, assetId },
  } = useRouter()

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  const { predictions } = data[name]

  return (
    <MetadataPrettyPredictionsContent name={name} predictions={predictions} />
  )
}

MetadataPrettyPredictionsQuery.propTypes = {
  name: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettyPredictionsQuery
