import PropTypes from 'prop-types'

import { colors, spacing, typography, constants } from '../Styles'

import { formatFullDate } from '../Date/helpers'

import Status from '../Status'
import ProgressBar from '../ProgressBar'

import DataQueueMenu from './Menu'

const DataQueueRow = ({
  job: {
    state,
    name,
    createdUser: { username },
    assetCounts,
    priority,
    timeCreated,
    timeStarted,
    timeUpdated,
    taskCounts: tC,
  },
}) => {
  const taskCounts = { ...tC, tasksPending: tC.tasksWaiting + tC.tasksQueued }

  return (
    <tr>
      <td>
        <Status jobStatus={state} />
      </td>
      <td>{name}</td>
      <td>{username}</td>
      <td css={{ textAlign: 'center' }}>{priority}</td>
      <td>{formatFullDate({ timestamp: timeCreated })}</td>
      <td css={{ textAlign: 'center' }}>
        {Object.values(assetCounts).reduce((total, count) => total + count)}
      </td>
      <td css={{ display: 'flex', justifyContent: 'center' }}>
        {assetCounts.assetErrorCount > 0 && (
          <div
            css={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 'fit-content',
              minWidth: spacing.spacious,
              height: spacing.spacious,
              padding: spacing.base,
              fontWeight: typography.weight.bold,
              fontSize: typography.size.kilo,
              lineHeight: typography.height.kilo,
              borderRadius: constants.borderRadius.round,
              color: colors.signal.warning.base,
              backgroundColor: colors.structure.coal,
            }}>
            {assetCounts.assetErrorCount}
          </div>
        )}
      </td>
      <td>
        <div css={{ display: 'flex', alignItems: 'center' }}>
          <ProgressBar
            state={state}
            taskCounts={taskCounts}
            timeStarted={timeStarted}
            timeUpdated={timeUpdated}
          />
          <div css={{ width: spacing.base }} />
          <DataQueueMenu />
        </div>
      </td>
    </tr>
  )
}

DataQueueRow.propTypes = {
  job: PropTypes.shape({
    id: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    createdUser: PropTypes.shape({
      username: PropTypes.string.isRequired,
    }).isRequired,
    assetCounts: PropTypes.shape({
      assetCreatedCount: PropTypes.number.isRequired,
      assetReplacedCount: PropTypes.number.isRequired,
      assetWarningCount: PropTypes.number.isRequired,
      assetErrorCount: PropTypes.number.isRequired,
    }).isRequired,
    priority: PropTypes.number.isRequired,
    timeCreated: PropTypes.number.isRequired,
    timeStarted: PropTypes.number.isRequired,
    timeUpdated: PropTypes.number.isRequired,
    taskCounts: PropTypes.shape({
      tasksFailure: PropTypes.number.isRequired,
      tasksSkipped: PropTypes.number.isRequired,
      tasksSuccess: PropTypes.number.isRequired,
      tasksRunning: PropTypes.number.isRequired,
    }).isRequired,
  }).isRequired,
}

export default DataQueueRow
