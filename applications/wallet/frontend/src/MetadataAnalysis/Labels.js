import PropTypes from 'prop-types'

import { constants, spacing } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataAnalysisBbox from './Bbox'
import MetadataAnalysisLabelDetection from './LabelDetection'

const MetadataAnalysisLabels = ({ name, value: { predictions } }) => {
  if (Object.keys(predictions[0]).includes('bbox')) {
    return (
      <div
        css={{
          '&:not(:first-of-type)': {
            borderTop: constants.borders.largeDivider,
          },
          '& > div[aria-label="Error Boundary"]': {
            padding: spacing.normal,
            paddingLeft: spacing.moderate,
            paddingRight: spacing.moderate,
          },
          '> div[aria-label="Error Boundary"]': {
            backgroundColor: 'transparent',
            boxShadow: 'none',
          },
        }}
      >
        <SuspenseBoundary>
          <MetadataAnalysisBbox name={name} />
        </SuspenseBoundary>
      </div>
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
