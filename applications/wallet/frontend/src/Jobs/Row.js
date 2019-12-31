import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

import { formatFullDate } from '../Date/helpers'

import Status from '../Status'
import ProgressBar from '../ProgressBar'

import JobsMenu from './Menu'

const ERROR_COUNT_HEIGHT = 32

const JobsRow = ({
  projectId,
  job: {
    id: jobId,
    state,
    name,
    createdUser: { username },
    assetCounts,
    priority,
    timeCreated,
    taskCounts: tC,
  },
  revalidate,
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
      <td>
        {assetCounts.assetErrorCount > 0 && (
          <div
            css={{
              margin: '0 auto',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 'fit-content',
              minWidth: ERROR_COUNT_HEIGHT,
              height: ERROR_COUNT_HEIGHT,
              padding: spacing.base,
              fontWeight: typography.weight.bold,
              fontSize: typography.size.kilo,
              lineHeight: typography.height.kilo,
              borderRadius: ERROR_COUNT_HEIGHT,
              color: colors.signal.warning.base,
              backgroundColor: colors.structure.coal,
            }}>
            {assetCounts.assetErrorCount}
          </div>
        )}
      </td>
      <td>
        <div css={{ display: 'flex', alignItems: 'center' }}>
          <ProgressBar taskCounts={taskCounts} />

          <div css={{ width: spacing.base }} />

          <JobsMenu
            projectId={projectId}
            jobId={jobId}
            revalidate={revalidate}
          />
        </div>
      </td>
    </tr>
  )
}

JobsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
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
  revalidate: PropTypes.func.isRequired,
}

export default JobsRow
