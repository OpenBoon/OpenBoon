import PropTypes from 'prop-types'
import { colors, spacing, constants } from '../Styles'

const STATUS_COLORS = {
  InProgress: colors.signal.canary.base,
  Cancelled: colors.structure.steel,
  Success: colors.signal.grass.base,
  Archived: colors.signal.grass.base,
  Failure: colors.signal.warning.base,
  Paused: colors.structure.coal,
}

const JobStatus = ({ jobStatus }) => {
  return (
    <div
      css={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: spacing.base,
        borderRadius: constants.borderRadius.small,
        color: STATUS_COLORS[jobStatus],
        backgroundColor:
          jobStatus === 'Paused'
            ? colors.signal.canary.base
            : colors.structure.coal,
        fontFamily: 'Roboto Condensed',
      }}>
      {jobStatus.replace(/([A-Z])/g, match => ` ${match}`).trim()}
    </div>
  )
}

JobStatus.propTypes = {
  jobStatus: PropTypes.oneOf(Object.keys(STATUS_COLORS)).isRequired,
}

export default JobStatus
