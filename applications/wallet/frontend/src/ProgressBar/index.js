import { useState } from 'react'
import PropTypes from 'prop-types'

import { constants, spacing, zIndex } from '../Styles'

import ProgressBarLegend from './Legend'

import { TASK_STATUS_COLORS } from './helpers'

const CONTAINER_HEIGHT = 16
export const CONTAINER_WIDTH = 212

const ProgressBar = ({ taskCounts }) => {
  const [showLegend, setShowLegend] = useState(false)

  return (
    <div
      aria-label="Progress Bar"
      role="button"
      tabIndex="0"
      onKeyPress={() => setShowLegend(!showLegend)}
      onMouseEnter={() => setShowLegend(true)}
      onMouseLeave={() => setShowLegend(false)}
      css={{
        display: 'flex',
        height: CONTAINER_HEIGHT,
        width: CONTAINER_WIDTH,
        position: 'relative',
      }}
    >
      <div css={{ display: 'flex', width: '100%' }}>
        {Object.keys(TASK_STATUS_COLORS)
          .filter((taskStatus) => taskCounts[taskStatus] > 0)
          .map((taskStatus) => {
            return (
              <div
                key={taskStatus}
                css={{
                  height: '100%',
                  flex: `${taskCounts[taskStatus]} 0 auto`,
                  backgroundColor: TASK_STATUS_COLORS[taskStatus],
                  '&:first-of-type': {
                    borderTopLeftRadius: constants.borderRadius.medium,
                    borderBottomLeftRadius: constants.borderRadius.medium,
                  },
                  '&:last-of-type': {
                    borderTopRightRadius: constants.borderRadius.medium,
                    borderBottomRightRadius: constants.borderRadius.medium,
                  },
                }}
              />
            )
          })}
      </div>
      {showLegend && (
        <div
          css={{
            position: 'absolute',
            top: CONTAINER_HEIGHT + spacing.base,
            right: 0,
            boxShadow: constants.boxShadows.tableRow,
            zIndex: zIndex.reset,
          }}
        >
          <ProgressBarLegend taskCounts={taskCounts} />
        </div>
      )}
    </div>
  )
}

ProgressBar.propTypes = {
  taskCounts: PropTypes.shape({
    tasksFailure: PropTypes.number.isRequired,
    tasksSkipped: PropTypes.number.isRequired,
    tasksSuccess: PropTypes.number.isRequired,
    tasksRunning: PropTypes.number.isRequired,
    tasksWaiting: PropTypes.number.isRequired,
    tasksQueued: PropTypes.number.isRequired,
  }).isRequired,
}

export default ProgressBar
