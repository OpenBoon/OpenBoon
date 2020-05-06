import PropTypes from 'prop-types'

import MetadataAnalysisFallback from './Fallback'
import MetadataAnalysisLabels from './Labels'
import MetadataAnalysisContentDetection from './ContentDetection'
import MetadataAnalysisSimilarityDetection from './SimilarityDetection'

const MetadataAnalysis = ({ name, value, path }) => {
  switch (value.type) {
    case 'labels':
      return (
        <MetadataAnalysisLabels name={name} predictions={value.predictions} />
      )
    case 'content':
      return (
        <MetadataAnalysisContentDetection name={name} content={value.content} />
      )
    case 'similarity':
      return (
        <MetadataAnalysisSimilarityDetection
          name={name}
          simhash={value.simhash}
        />
      )
    default:
      return <MetadataAnalysisFallback name={name} value={value} path={path} />
  }
}

MetadataAnalysis.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    type: PropTypes.string.isRequired,
    predictions: PropTypes.arrayOf(PropTypes.shape({})),
    content: PropTypes.string.isRequired,
    simhash: PropTypes.string.isRequired,
  }).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataAnalysis
