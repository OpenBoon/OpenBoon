import PropTypes from 'prop-types'
import { colors, spacing, constants } from '../Styles'

const STATUS_COLORS = {
  Active: colors.signal.canary.base,
  Cancelled: colors.structure.steel,
  Finished: colors.signal.grass.base,
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
