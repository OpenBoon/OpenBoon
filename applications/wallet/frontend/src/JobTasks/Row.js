import PropTypes from 'prop-types'
import Link from 'next/link'

import { onRowClickRouterPush } from '../Table/helpers'
import { formatFullDate, getDuration, formatDuration } from '../Date/helpers'
import { spacing, typography } from '../Styles'

import JobTasksStateIcon from './StateIcon'

const JobTasksRow = ({
  projectId,
  jobId,
  task: {
    id: taskId,
    name,
    state,
    timeStarted,
    timeStopped,
    assetCounts: { assetErrorCount },
  },
}) => {
  const isStarted = timeStarted !== -1
  const taskDuration = getDuration({
    timeStarted,
    timeStopped,
    now: Date.now(),
  })

  return (
    <tr
      css={{ cursor: 'pointer' }}
      onClick={onRowClickRouterPush(
        '/[projectId]/jobs/[jobId]/tasks/[taskId]',
        `/${projectId}/jobs/${jobId}/tasks/${taskId}`,
      )}
    >
      <td css={{ display: 'flex' }}>
        <JobTasksStateIcon state={state} />
        <span css={{ paddingLeft: spacing.normal }}>{state}</span>
      </td>

      <td>
        <Link
          href="/[projectId]/jobs/[jobId]/tasks/[taskId]"
          as={`/${projectId}/jobs/${jobId}/tasks/${taskId}`}
          passHref
        >
          <a css={{ ':hover': { textDecoration: 'none' } }} title={name}>
            {name}
          </a>
        </Link>
      </td>

      <td>
        {isStarted ? (
          formatFullDate({ timestamp: timeStarted })
        ) : (
          <div
            css={{ fontStyle: typography.style.italic }}
          >{`${state}...`}</div>
        )}
      </td>

      <td>
        {isStarted ? (
          formatDuration({
            seconds: taskDuration / 1000,
          })
        ) : (
          <div
            css={{ fontStyle: typography.style.italic }}
          >{`${state}...`}</div>
        )}
      </td>

      <td>{assetErrorCount}</td>
    </tr>
  )
}

JobTasksRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
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
  }).isRequired,
}

export default JobTasksRow
