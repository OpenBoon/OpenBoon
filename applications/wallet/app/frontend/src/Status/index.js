import PropTypes from 'prop-types'
import { colors, spacing, constants } from '../Styles'

const STATUS_COLORS = {
  // running color
  Active: colors.signal.canary.base,
  InProgress: colors.signal.canary.base,
  Paused: colors.signal.canary.base,

  // failure color
  Cancelled: colors.signal.warning.base,
  Failure: colors.signal.warning.base,

  // success color
  Success: colors.signal.grass.base,
  Finished: colors.signal.grass.base,
  Archived: colors.signal.grass.base,
}

const Status = ({ jobStatus }) => {
  return (
    <div
      css={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: spacing.base,
        borderRadius: constants.borderRadius.small,
        color: STATUS_COLORS[jobStatus],
        backgroundColor: colors.structure.coal,
      }}>
      {jobStatus}
    </div>
  )
}

Status.propTypes = {
  jobStatus: PropTypes.oneOf(Object.keys(STATUS_COLORS)).isRequired,
}

export default Status
