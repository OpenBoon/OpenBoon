import PropTypes from 'prop-types'
import { colors, constants } from '../Styles'

const CONTAINER_HEIGHT = 16
const CONTAINER_WIDTH = 200
const STATUS_COLORS = {
  tasksSuccess: colors.green1,
  tasksFailure: colors.error,
  tasksRunning: colors.blue1,
  tasksWaiting: colors.grey6,
}

const ProgressBar = ({ state, taskCounts }) => {
  if (state === 'Canceled') {
    return <div css={{ color: colors.grey5 }}>Canceled</div>
  }

  return (
    <div
      css={{
        display: 'flex',
        height: CONTAINER_HEIGHT,
        width: CONTAINER_WIDTH,
      }}>
      {Object.keys(STATUS_COLORS)
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
                backgroundColor: STATUS_COLORS[taskStatus],
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
    </div>
  )
}

ProgressBar.propTypes = {
  state: PropTypes.string.isRequired,
  taskCounts: PropTypes.shape({
    tasksSuccess: PropTypes.number,
    tasksFailure: PropTypes.number,
    tasksRunning: PropTypes.number,
    tasksWaiting: PropTypes.number,
  }).isRequired,
}

export default ProgressBar
