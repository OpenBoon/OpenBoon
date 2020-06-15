import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

const VisualizerNavigation = ({ itemCount }) => {
  return (
    <div
      css={{
        padding: spacing.base,
        alignItems: 'center',
        fontFamily: 'Roboto Condensed',
        fontSize: typography.size.regular,
        lineHeight: typography.height.regular,
        backgroundColor: colors.structure.lead,
        color: colors.structure.steel,
        boxShadow: constants.boxShadows.navBar,
        marginBottom: spacing.hairline,
      }}
    >
      {itemCount} Assets
    </div>
  )
}

VisualizerNavigation.propTypes = {
  itemCount: PropTypes.number.isRequired,
}

export default VisualizerNavigation
