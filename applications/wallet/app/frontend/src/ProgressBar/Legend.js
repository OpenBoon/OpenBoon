import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import { getTimeEnded, TASK_STATUS_COLORS } from './helpers'
import { getDuration } from '../Date/helpers'

import ClockSvg from '../Icons/clock.svg'

const TASK_STATUS_LABELS = {
  tasksFailure: 'Failed',
  tasksSkipped: 'Skipped',
  tasksSuccess: 'Succeeded',
  tasksRunning: 'Running',
  tasksPending: 'Pending',
}

const ProgressBarLegend = ({
  state,
  tasksProgress,
  timeStarted,
  timeUpdated,
}) => {
  const currentTime = Date.now()
  const timeEnded = getTimeEnded({ state, currentTime, timeUpdated })
  const duration = getDuration({ timeStarted, timeEnded })

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
                <div>{`${TASK_STATUS_LABELS[statusName]}:`}</div>
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

ProgressBarLegend.propTypes = {
  state: PropTypes.string.isRequired,
  timeStarted: PropTypes.number.isRequired,
  timeUpdated: PropTypes.number.isRequired,
  tasksProgress: PropTypes.shape({
    Failed: PropTypes.number,
    Skipped: PropTypes.number,
    Succeeded: PropTypes.number,
    Running: PropTypes.number,
    Pending: PropTypes.number,
  }).isRequired,
}

export default ProgressBarLegend
