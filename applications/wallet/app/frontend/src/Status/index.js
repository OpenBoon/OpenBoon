import PropTypes from 'prop-types'
import { colors, spacing, constants } from '../Styles'

const STATUS_COLORS = {
  Active: colors.signal.canary.base,
  Finished: colors.signal.grass.base,

  // new job states
  InProgress: colors.signal.canary.base,
  Cancelled: colors.signal.warning.base,
  Success: colors.signal.grass.base,
  Archived: colors.signal.grass.base,
  Failure: colors.signal.warning.base,
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
      {jobStatus.replace(/([A-Z])/g, match => ` ${match}`)}
    </div>
  )
}

Status.propTypes = {
  jobStatus: PropTypes.oneOf(Object.keys(STATUS_COLORS)).isRequired,
}

export default Status
