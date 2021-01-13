import PropTypes from 'prop-types'

const settingsShape = {
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  labelsWidth: PropTypes.number.isRequired,
  zoom: PropTypes.number.isRequired,
  isMinimapOpen: PropTypes.bool.isRequired,
  isPreviewOpen: PropTypes.bool.isRequired,
  selectedCell: PropTypes.arrayOf(PropTypes.number.isRequired).isRequired,
  isNormalized: PropTypes.bool.isRequired,
  minScore: PropTypes.number.isRequired,
  maxScore: PropTypes.number.isRequired,
}

export default settingsShape
