import PropTypes from 'prop-types'

import ProgressBar from '../ProgressBar'

import { spacing } from '../Styles'

const JobDetailsStatus = ({ taskCounts }) => {
  return (
    <>
      <div
        css={{
          paddingBottom: `${spacing.moderate}px`,
        }}>
        Task Progress:
      </div>

      <ProgressBar taskCounts={taskCounts} containerWidth={444} />
    </>
  )
}

JobDetailsStatus.propTypes = {
  taskCounts: PropTypes.shape({
    tasksFailure: PropTypes.number.isRequired,
    tasksSkipped: PropTypes.number.isRequired,
    tasksSuccess: PropTypes.number.isRequired,
    tasksRunning: PropTypes.number.isRequired,
  }).isRequired,
}

export default JobDetailsStatus
