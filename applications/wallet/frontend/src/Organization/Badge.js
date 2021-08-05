import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

const LETTER_SPACING = 0.37

const OrganizationBadge = ({ children }) => {
  return (
    <div css={{ display: 'flex' }}>
      <div
        css={{
          backgroundColor: colors.structure.white,
          borderRadius: constants.borderRadius.round,
          padding: spacing.base,
          paddingTop: 0,
          paddingBottom: 0,
          color: colors.structure.soot,
          fontSize: typography.size.invisible,
          lineHeight: typography.height.invisible,
          fontWeight: typography.weight.medium,
          textTransform: 'uppercase',
          letterSpacing: LETTER_SPACING,
        }}
      >
        {children.replaceAll('_', ' ')} plan
      </div>
    </div>
  )
}

OrganizationBadge.propTypes = {
  children: PropTypes.node.isRequired,
}

export default OrganizationBadge
