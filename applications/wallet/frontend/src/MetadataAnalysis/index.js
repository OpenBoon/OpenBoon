import PropTypes from 'prop-types'

import MetadataPrettyLabels from '../MetadataPretty/Labels'
import MetadataPrettyContent from '../MetadataPretty/Content'
import MetadataPrettySimilarity from '../MetadataPretty/Similarity'

const MetadataAnalysis = ({ name, value }) => {
  switch (value.type) {
    case 'labels':
      return <MetadataPrettyLabels name={name} value={value} />

    case 'content':
      return <MetadataPrettyContent name={name} value={value} />

    case 'similarity':
      return <MetadataPrettySimilarity name={name} value={value} />

    default:
      return null
  }
}

MetadataAnalysis.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({ type: PropTypes.string.isRequired }).isRequired,
}

export default MetadataAnalysis
