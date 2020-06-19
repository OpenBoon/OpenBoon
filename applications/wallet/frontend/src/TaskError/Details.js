import { useRouter } from 'next/router'
import PropTypes from 'prop-types'

import { formatFullDate } from '../Date/helpers'

import Value, { VARIANTS } from '../Value'

import { spacing } from '../Styles'

const TaskErrorDetails = ({
  taskError: { jobId, taskId, analyst, phase, path, processor, timeCreated },
}) => {
  const {
    query: { errorId },
  } = useRouter()

  return (
    <div
      css={{
        display: 'flex',
        flexWrap: 'wrap',
        paddingTop: spacing.normal,
      }}
    >
      <div css={{ paddingRight: spacing.colossal }}>
        <Value legend="Job ID" variant={VARIANTS.SECONDARY}>
          {jobId}
        </Value>

        <Value legend="Task ID" variant={VARIANTS.SECONDARY}>
          {taskId}
        </Value>

        <Value legend="Error ID" variant={VARIANTS.SECONDARY}>
          {errorId}
        </Value>

        <Value legend="Host ID" variant={VARIANTS.SECONDARY}>
          {analyst}
        </Value>
      </div>
      <div>
        <Value legend="Phase" variant={VARIANTS.SECONDARY}>
          {phase}
        </Value>

        <Value legend="File Path" variant={VARIANTS.SECONDARY}>
          {path}
        </Value>

        <Value legend="Processor" variant={VARIANTS.SECONDARY}>
          {processor}
        </Value>

        <Value legend="Time of Error" variant={VARIANTS.SECONDARY}>
          {formatFullDate({ timestamp: timeCreated })}
        </Value>
      </div>
    </div>
  )
}

TaskErrorDetails.propTypes = {
  taskError: PropTypes.shape({
    jobId: PropTypes.string.isRequired,
    taskId: PropTypes.string.isRequired,
    analyst: PropTypes.string.isRequired,
    phase: PropTypes.string.isRequired,
    path: PropTypes.string.isRequired,
    processor: PropTypes.string.isRequired,
    timeCreated: PropTypes.number.isRequired,
  }).isRequired,
}

export default TaskErrorDetails
