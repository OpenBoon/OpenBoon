import PropTypes from 'prop-types'
import { colors, spacing, constants } from '../Styles'

const LABEL_COLORS = {
  Active: colors.yellow1,
  Paused: colors.structureShades.black,
  Canceled: colors.structureShades.steel,
  Finished: colors.signalColors.grass,
}

const LABEL_HEIGHT = 24

const Status = ({ jobStatus }) => {
  return (
    <div
      css={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: spacing.base,
        height: LABEL_HEIGHT,
        borderRadius: constants.borderRadius.small,
        color: LABEL_COLORS[jobStatus],
        backgroundColor:
          jobStatus === 'Paused' ? colors.yellow1 : colors.structureShades.coal,
      }}>
      {jobStatus}
    </div>
  )
}

Status.propTypes = {
  jobStatus: PropTypes.oneOf(Object.keys(LABEL_COLORS)).isRequired,
}

export default Status
