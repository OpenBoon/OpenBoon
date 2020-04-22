import PropTypes from 'prop-types'

import { spacing, colors, typography } from '../Styles'

const IMG_SPACING = 44

const AccountUsage = ({ usage, limit, legend }) => {
  const available = limit - usage

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        paddingLeft: IMG_SPACING,
        fontFamily: 'Roboto Condensed',
      }}
    >
      <div
        css={{
          color: colors.structure.zinc,
          paddingBottom: spacing.mini,
        }}
      >
        Used:{' '}
        <span css={{ fontWeight: typography.weight.bold }}>
          {usage.toLocaleString()}
          {legend}
        </span>
      </div>
      <div css={{ color: colors.key.four }}>
        Available:{' '}
        <span css={{ fontWeight: typography.weight.bold }}>
          {available.toLocaleString()}
          {legend}
        </span>
      </div>
    </div>
  )
}

AccountUsage.propTypes = {
  limit: PropTypes.number.isRequired,
  usage: PropTypes.number.isRequired,
  legend: PropTypes.string.isRequired,
}

export default AccountUsage
