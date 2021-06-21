import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'
import { decamelize } from '../Text/helpers'

const STYLES = {
  LARGE: {
    fontSize: typography.size.medium,
    lineHeight: typography.height.medium,
    padding: spacing.normal,
    paddingLeft: spacing.comfy,
    paddingRight: spacing.comfy,
  },
  SMALL: {
    fontSize: typography.size.regular,
    lineHeight: typography.height.regular,
    padding: spacing.small,
    paddingLeft: spacing.base,
    paddingRight: spacing.base,
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const FONT_COLORS = {
  InProgress: colors.signal.canary.base,
  Paused: colors.structure.soot,
  Cancelled: colors.structure.zinc,
  Success: colors.structure.soot,
  Archived: colors.structure.zinc,
  Failure: colors.structure.soot,
}

const BACKGROUND_COLORS = {
  InProgress: colors.structure.smoke,
  Paused: colors.signal.canary.base,
  Cancelled: colors.structure.smoke,
  Success: colors.signal.grass.base,
  Archived: colors.structure.smoke,
  Failure: colors.signal.warning.base,
}

const JobStatus = ({ variant, status }) => {
  return (
    <div css={{ display: 'flex' }}>
      <div
        css={{
          color: FONT_COLORS[status],
          backgroundColor: BACKGROUND_COLORS[status],
          fontWeight: typography.weight.medium,
          borderRadius: constants.borderRadius.round,
          whiteSpace: 'nowrap',
          ...STYLES[variant],
        }}
      >
        {variant === 'LARGE' && 'Job'} {decamelize({ word: status })}
      </div>
    </div>
  )
}

JobStatus.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  status: PropTypes.oneOf([
    'InProgress',
    'Paused',
    'Cancelled',
    'Success',
    'Archived',
    'Failure',
  ]).isRequired,
}

export default JobStatus
