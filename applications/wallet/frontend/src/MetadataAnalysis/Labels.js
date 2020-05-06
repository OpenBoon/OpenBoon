import PropTypes from 'prop-types'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataAnalysisBbox from './Bbox'
import MetadataAnalysisLabelDetection from './LabelDetection'

const MetadataAnalysisLabels = ({ name, value: { predictions } }) => {
  if (Object.keys(predictions[0]).includes('bbox')) {
    return (
      <SuspenseBoundary>
        <MetadataAnalysisBbox name={name} />
      </SuspenseBoundary>
    )
  }
  return <MetadataAnalysisLabelDetection name={name} value={{ predictions }} />
}

MetadataAnalysisLabels.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    predictions: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  }).isRequired,
}

export default MetadataAnalysisLabels
