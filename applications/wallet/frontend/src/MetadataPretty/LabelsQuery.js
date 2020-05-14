import PropTypes from 'prop-types'

import { useRouter } from 'next/router'
import useSWR from 'swr'

import MetadataPrettyLabelsContent, { BBOX_SIZE } from './LabelsContent'

const MetadataPrettyLabelsQuery = ({ name }) => {
  const attr = `analysis.${name}&width=${BBOX_SIZE}`

  const {
    query: { projectId, id: assetId },
  } = useRouter()

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  return <MetadataPrettyLabelsContent name={name} value={data[name]} />
}

MetadataPrettyLabelsQuery.propTypes = {
  name: PropTypes.string.isRequired,
}

export default MetadataPrettyLabelsQuery
