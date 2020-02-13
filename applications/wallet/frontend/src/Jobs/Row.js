import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing, typography } from '../Styles'

import { formatFullDate } from '../Date/helpers'

import Status from '../Status'
import ProgressBar, { CONTAINER_WIDTH } from '../ProgressBar'

import JobsMenu from './Menu'

const ERROR_COUNT_HEIGHT = 32

const JobsRow = ({
  projectId,
  job: {
    id: jobId,
    state,
    name,
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
      <td css={{ textAlign: 'center' }}>{priority}</td>
      <td>{formatFullDate({ timestamp: timeCreated })}</td>
      <td css={{ textAlign: 'center' }}>
        {Object.values(assetCounts).reduce((total, count) => total + count)}
      </td>
      <td>
        {assetCounts.assetErrorCount > 0 && (
          <Link
            href="/[projectId]/jobs/[jobId]/errors"
            as={`/${projectId}/jobs/${jobId}/errors`}
            passHref>
            <a
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
                fontSize: typography.size.regular,
                lineHeight: typography.height.regular,
                borderRadius: ERROR_COUNT_HEIGHT,
                color: colors.signal.warning.base,
                backgroundColor: colors.structure.coal,
                '&:hover': {
                  textDecoration: 'none',
                  cursor: 'pointer',
                },
              }}>
              {assetCounts.assetErrorCount}
            </a>
          </Link>
        )}
      </td>
      <td
        style={{
          minWidth: CONTAINER_WIDTH + spacing.normal * 2,
          overflow: 'visible',
        }}>
        <ProgressBar taskCounts={taskCounts} />
      </td>
      <td>
        <JobsMenu projectId={projectId} jobId={jobId} revalidate={revalidate} />
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
