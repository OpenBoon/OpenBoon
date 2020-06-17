import PropTypes from 'prop-types'

import { colors, spacing, constants, typography } from '../Styles'

const STATUS_COLORS = {
  InProgress: colors.signal.canary.base,
  Cancelled: colors.structure.zinc,
  Success: colors.signal.grass.base,
  Archived: colors.structure.iron,
  Failure: colors.signal.warning.base,
  Paused: colors.structure.coal,
}

const JobsStatus = ({ status }) => {
  return (
    <div
      css={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: spacing.base,
        borderRadius: constants.borderRadius.small,
        color: STATUS_COLORS[status],
        backgroundColor:
          status === 'Paused'
            ? colors.signal.canary.base
            : colors.structure.coal,
        fontFamily: typography.family.condensed,
      }}
    >
      {status.replace(/([A-Z])/g, (match) => ` ${match}`).trim()}
    </div>
  )
}

JobsStatus.propTypes = {
  status: PropTypes.oneOf(Object.keys(STATUS_COLORS)).isRequired,
}

export default JobsStatus
