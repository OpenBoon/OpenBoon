import PropTypes from 'prop-types'

import { useRouter } from 'next/router'
import useSWR from 'swr'

import MetadataAnalysisLabelDetection, { BBOX_SIZE } from './LabelDetection'

const MetadataAnalysisBbox = ({ name }) => {
  const attr = `analysis.${name}&width=${BBOX_SIZE}`

  const {
    query: { projectId, id: assetId },
  } = useRouter()

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  return <MetadataAnalysisLabelDetection name={name} value={data[name]} />
}

MetadataAnalysisBbox.propTypes = {
  name: PropTypes.string.isRequired,
}

export default MetadataAnalysisBbox
