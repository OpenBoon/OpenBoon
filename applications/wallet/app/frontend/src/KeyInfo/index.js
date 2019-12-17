import PropTypes from 'prop-types'
import { calculateDuration } from '../Date/helpers'
import { colors, constants, spacing, typography } from '../Styles'
import { TASK_STATUS_COLORS } from '../ProgressBar/helpers'
import ClockSvg from '../Icons/clock.svg'

const InfoKey = ({ tasksProgress, timeStarted }) => {
  const duration = calculateDuration({ timestamp: timeStarted })

  return (
    <div
      css={{
        display: 'flex',
        padding: spacing.moderate,
        backgroundColor: colors.structure.iron,
        borderRadius: constants.borderRadius.small,
      }}>
      <div css={{ display: 'flex' }}>
        <div css={{ display: 'flex' }}>
          <div css={{ paddingRight: spacing.base }}>
            <ClockSvg width={20} color={colors.structure.steel} />
          </div>
          <div css={{ paddingRight: spacing.comfy }}>
            <div css={{ color: colors.structure.pebble }}>
              <div>Duration:</div>
              <div
                css={{
                  color: colors.structure.white,
                  fontWeight: typography.weight.bold,
                }}>{`${duration.hours} hr / ${duration.minutes} m`}</div>
            </div>
          </div>
        </div>
        {Object.keys(TASK_STATUS_COLORS).map(statusName => {
          return (
            <div
              key={statusName}
              css={{ display: 'flex', paddingRight: spacing.comfy }}>
              <div
                css={{
                  width: 2,
                  height: '100%',
                  backgroundColor: TASK_STATUS_COLORS[statusName],
                }}
              />
              <div
                css={{
                  color: colors.structure.pebble,
                  paddingLeft: spacing.base,
                }}>
                <div>{`${statusName}:`}</div>
                <div
                  css={{
                    color: colors.structure.white,
                    fontWeight: typography.weight.bold,
                  }}>
                  {tasksProgress[statusName]}
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

InfoKey.propTypes = {
  tasksProgress: PropTypes.shape({
    Failed: PropTypes.number,
    Skipped: PropTypes.number,
    Succeeded: PropTypes.number,
    Running: PropTypes.number,
    Pending: PropTypes.number,
  }).isRequired,
  timeStarted: PropTypes.number.isRequired,
}

export default InfoKey
