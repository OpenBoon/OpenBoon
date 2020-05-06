import PropTypes from 'prop-types'

import MetadataAnalysisLabels from './Labels'
import MetadataAnalysisContentDetection from './ContentDetection'
import MetadataAnalysisSimilarityDetection from './SimilarityDetection'

const MetadataAnalysis = ({ name, value }) => {
  switch (value.type) {
    case 'labels':
      return <MetadataAnalysisLabels name={name} value={value} />
    case 'content':
      return <MetadataAnalysisContentDetection name={name} value={value} />
    // set similarity detection to default
    default:
      return <MetadataAnalysisSimilarityDetection name={name} value={value} />
  }
}

MetadataAnalysis.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({ type: PropTypes.string.isRequired }).isRequired,
}

export default MetadataAnalysis
