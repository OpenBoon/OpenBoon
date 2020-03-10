import PropTypes from 'prop-types'

import { constants, colors, spacing } from '../Styles'

const VisualizerInfobar = ({ displayCount, totalCount }) => (
  <div
    css={{
      minHeight: constants.navbar.height,
      display: 'flex',
      alignItems: 'center',
      backgroundColor: colors.structure.mattGrey,
      boxShadow: constants.boxShadows.infoBar,
      fontFamily: 'Roboto Condensed',
    }}>
    <div css={{ flex: 1 }} />
    <div css={{ color: colors.structure.steel, padding: spacing.base }}>
      Sort: Import Date
    </div>
    <div css={{ color: colors.structure.steel }}>|</div>
    <div
      css={{
        color: colors.structure.zinc,
        padding: spacing.base,
        paddingRight: spacing.normal,
      }}>
      {displayCount} of {totalCount} Results
    </div>
  </div>
)

VisualizerInfobar.propTypes = {
  displayCount: PropTypes.number.isRequired,
  totalCount: PropTypes.number.isRequired,
}

export default VisualizerInfobar
