import PropTypes from 'prop-types'

import { formatFullDate } from '../Date/helpers'

import Value, { VARIANTS } from '../Value'

import { spacing } from '../Styles'

const JobErrorDetails = ({
  taskId,
  errorId,
  analyst,
  path,
  processor,
  timeCreated,
}) => {
  return (
    <div
      css={{
        display: 'flex',
        maxWidth: '1440px',
        paddingTop: spacing.normal,
        flexWrap: 'wrap',
      }}>
      <div css={{ paddingRight: spacing.colossal }}>
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

JobErrorDetails.propTypes = {
  taskId: PropTypes.string.isRequired,
  errorId: PropTypes.string.isRequired,
  analyst: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
  processor: PropTypes.string.isRequired,
  timeCreated: PropTypes.number.isRequired,
}

export default JobErrorDetails
