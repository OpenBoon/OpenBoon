import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants } from '../Styles'

import MetadataPrettyPredictionsContent from './PredictionsContent'

const MetadataPrettyPredictionsQuery = ({ path, name, type }) => {
  const attr = `${path}.${name}&width=${constants.bbox}`

  const {
    query: { projectId, assetId },
  } = useRouter()

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  const { predictions } = data[name]

  return (
    <MetadataPrettyPredictionsContent
      path={path}
      name={name}
      type={type}
      predictions={predictions}
    />
  )
}

MetadataPrettyPredictionsQuery.propTypes = {
  path: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  type: PropTypes.oneOf(['labels', 'text']).isRequired,
}

export default MetadataPrettyPredictionsQuery
