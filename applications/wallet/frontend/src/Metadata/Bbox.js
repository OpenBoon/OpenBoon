import PropTypes from 'prop-types'

import { useRouter } from 'next/router'
import useSWR from 'swr'

import MetadataLabelDetection, { BBOX_SIZE } from './LabelDetection'

const MetadataBbox = ({ name }) => {
  const attr = `analysis.${name}&width=${BBOX_SIZE}`

  const {
    query: { projectId, id: assetId },
  } = useRouter()

  const {
    data: {
      [name]: { predictions },
    },
  } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  return <MetadataLabelDetection name={name} predictions={predictions} />
}

MetadataBbox.propTypes = {
  name: PropTypes.string.isRequired,
}

export default MetadataBbox
