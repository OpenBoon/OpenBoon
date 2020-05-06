import PropTypes from 'prop-types'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataAnalysisBbox from './Bbox'
import MetadataAnalysisLabelDetection from './LabelDetection'

const MetadataAnalysisLabels = ({ name, predictions }) => {
  if (Object.keys(predictions[0]).includes('bbox')) {
    return (
      <SuspenseBoundary>
        <MetadataAnalysisBbox name={name} />
      </SuspenseBoundary>
    )
  }
  return (
    <MetadataAnalysisLabelDetection name={name} predictions={predictions} />
  )
}

MetadataAnalysisLabels.propTypes = {
  name: PropTypes.string.isRequired,
  predictions: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
}

export default MetadataAnalysisLabels
