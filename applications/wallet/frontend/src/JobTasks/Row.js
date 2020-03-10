import PropTypes from 'prop-types'

import { formatFullDate, formatDuration } from '../Date/helpers'

const JobTasksRow = ({
  task: {
    id,
    name,
    state,
    timeStarted,
    timeStopped,
    timePing,
    assetCounts: { assetErrorCount, assetTotalCount },
  },
}) => {
  return (
    <tr>
      <td>{state}</td>
      <td title={id}>{id}</td>
      <td title={name}>{name}</td>
      <td>{formatDuration({ seconds: timeStopped - timeStarted })}</td>
      <td>{assetTotalCount}</td>
      <td>{formatFullDate({ timestamp: timePing })}</td>
      <td>{assetErrorCount}</td>
    </tr>
  )
}

JobTasksRow.propTypes = {
  task: PropTypes.shape({
    id: PropTypes.string.isRequired,
    jobId: PropTypes.string.isRequired,
    projectId: PropTypes.string.isRequired,
    dataSourceId: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
    host: PropTypes.string.isRequired,
    timeStarted: PropTypes.number.isRequired,
    timeStopped: PropTypes.number.isRequired,
    timeCreated: PropTypes.number.isRequired,
    timePing: PropTypes.number.isRequired,
    assetCounts: PropTypes.shape({
      assetCreatedCount: PropTypes.number.isRequired,
      assetReplacedCount: PropTypes.number.isRequired,
      assetWarningCount: PropTypes.number.isRequired,
      assetErrorCount: PropTypes.number.isRequired,
      assetTotalCount: PropTypes.number.isRequired,
    }).isRequired,
    taskId: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
  }).isRequired,
}

export default JobTasksRow
