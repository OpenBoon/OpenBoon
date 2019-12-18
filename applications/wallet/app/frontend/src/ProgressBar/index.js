import { useState } from 'react'
import PropTypes from 'prop-types'

import { constants, spacing, zIndex } from '../Styles'

import ProgressBarLegend from './Legend'

import { TASK_STATUS_COLORS } from './helpers'

const CONTAINER_HEIGHT = 16
const CONTAINER_WIDTH = 212

const ProgressBar = ({ state, taskCounts, timeStarted, timeUpdated }) => {
  const tasksProgress = {
    ...taskCounts,
    tasksPending: taskCounts.tasksWaiting + taskCounts.tasksQueued,
  }
  const [showLegend, setShowLegend] = useState()

  return (
    <div
      aria-label="Progress Bar"
      role="button"
      tabIndex="0"
      onKeyPress={() => setShowLegend(!showLegend)}
      onMouseEnter={() => setShowLegend(true)}
      onMouseLeave={() => setShowLegend(false)}
      css={{
        position: 'relative',
        display: 'flex',
        height: CONTAINER_HEIGHT,
        width: CONTAINER_WIDTH,
      }}>
      {Object.keys(TASK_STATUS_COLORS)
        .filter(taskStatus => {
          return taskCounts[taskStatus] > 0
        })
        .map(taskStatus => {
          return (
            <div
              key={taskStatus}
              css={{
                height: '100%',
                flex: `${taskCounts[taskStatus]} 0 auto`,
                backgroundColor: TASK_STATUS_COLORS[taskStatus],
                '&:first-of-type': {
                  borderTopLeftRadius: constants.borderRadius.small,
                  borderBottomLeftRadius: constants.borderRadius.small,
                },
                '&:last-of-type': {
                  borderTopRightRadius: constants.borderRadius.small,
                  borderBottomRightRadius: constants.borderRadius.small,
                },
              }}
            />
          )
        })}
      {showLegend && (
        <div
          css={{
            position: 'absolute',
            top: CONTAINER_HEIGHT + spacing.base,
            right: 0,
            boxShadow: constants.boxShadows.tableRow,
            zIndex: zIndex.layout.dropdown,
          }}>
          <ProgressBarLegend
            state={state}
            taskCounts={taskCounts}
            timeStarted={timeStarted}
            timeUpdated={timeUpdated}
          />
        </div>
      )}
    </div>
  )
}

ProgressBar.propTypes = {
  state: PropTypes.string.isRequired,
  timeStarted: PropTypes.number.isRequired,
  timeUpdated: PropTypes.number.isRequired,
  taskCounts: PropTypes.shape({
    tasksFailure: PropTypes.number,
    tasksSkipped: PropTypes.number,
    tasksSuccess: PropTypes.number,
    tasksRunning: PropTypes.number,
    tasksWaiting: PropTypes.number,
    tasksQueued: PropTypes.number,
  }).isRequired,
}

export default ProgressBar
