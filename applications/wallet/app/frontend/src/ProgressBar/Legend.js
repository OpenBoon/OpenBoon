import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import { TASK_STATUS_COLORS } from './helpers'

const TASK_STATUS_LABELS = {
  tasksFailure: 'Failed',
  tasksSkipped: 'Skipped',
  tasksSuccess: 'Succeeded',
  tasksRunning: 'Running',
  tasksPending: 'Pending',
}

const ProgressBarLegend = ({ taskCounts }) => {
  return (
    <div
      css={{
        display: 'flex',
        padding: spacing.moderate,
        backgroundColor: colors.structure.iron,
        borderRadius: constants.borderRadius.small,
      }}>
      <div css={{ display: 'flex' }}>
        {Object.keys(TASK_STATUS_COLORS).map(statusName => {
          return (
            <div
              key={statusName}
              css={{
                display: 'flex',
                ':not(:last-of-type)': { paddingRight: spacing.comfy },
              }}>
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
                  {taskCounts[statusName]}
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
  taskCounts: PropTypes.shape({
    tasksFailure: PropTypes.number.isRequired,
    tasksSkipped: PropTypes.number.isRequired,
    tasksSuccess: PropTypes.number.isRequired,
    tasksRunning: PropTypes.number.isRequired,
    tasksPending: PropTypes.number.isRequired,
  }).isRequired,
}

export default ProgressBarLegend
