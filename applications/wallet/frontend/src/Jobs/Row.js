import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing } from '../Styles'

import { onRowClickRouterPush } from '../Table/helpers'
import { formatFullDate } from '../Date/helpers'

import WarningSvg from '../Icons/warning.svg'

import ProgressBar, { CONTAINER_WIDTH } from '../ProgressBar'
import JobStatus, { VARIANTS as JOB_STATUS_VARIANTS } from '../Job/Status'

import JobsMenu from './Menu'

const ERROR_COUNT_HEIGHT = 32

const JobsRow = ({
  projectId,
  job: {
    id: jobId,
    state,
    paused,
    name,
    assetCounts,
    priority,
    timeCreated,
    taskCounts: tC,
  },
  revalidate,
}) => {
  const taskCounts = { ...tC, tasksPending: tC.tasksWaiting + tC.tasksQueued }

  const status = paused ? 'Paused' : state

  return (
    <tr
      css={{ cursor: 'pointer' }}
      onClick={onRowClickRouterPush(
        '/[projectId]/jobs/[jobId]',
        `/${projectId}/jobs/${jobId}`,
      )}
    >
      <td>
        <JobStatus variant={JOB_STATUS_VARIANTS.SMALL} status={status} />
      </td>

      <td>
        <Link
          href="/[projectId]/jobs/[jobId]"
          as={`/${projectId}/jobs/${jobId}`}
          passHref
        >
          <a css={{ ':hover': { textDecoration: 'none' } }}>{name}</a>
        </Link>
      </td>

      <td css={{ textAlign: 'center' }}>{priority}</td>

      <td>{formatFullDate({ timestamp: timeCreated })}</td>

      <td css={{ textAlign: 'center' }}>{assetCounts.assetTotalCount}</td>

      <td>
        {assetCounts.assetErrorCount > 0 && (
          <div css={{ display: 'flex', justifyContent: 'center' }}>
            <Link
              href="/[projectId]/jobs/[jobId]/errors"
              as={`/${projectId}/jobs/${jobId}/errors`}
              passHref
            >
              <a
                css={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 'fit-content',
                  minWidth: ERROR_COUNT_HEIGHT,
                  height: ERROR_COUNT_HEIGHT,
                  padding: spacing.moderate / 2,
                  margin: -spacing.moderate / 2,
                  borderRadius: ERROR_COUNT_HEIGHT,
                  color: colors.signal.warning.base,
                  '&:hover': {
                    textDecoration: 'none',
                    cursor: 'pointer',
                    backgroundColor: colors.structure.coal,
                  },
                }}
              >
                <WarningSvg height={20} color={colors.signal.warning.base} />
              </a>
            </Link>
          </div>
        )}
      </td>

      <td
        style={{
          minWidth: CONTAINER_WIDTH + spacing.normal * 2,
          overflow: 'visible',
        }}
      >
        <ProgressBar taskCounts={taskCounts} />
      </td>

      <td>
        <JobsMenu
          projectId={projectId}
          jobId={jobId}
          status={status}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

JobsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  job: PropTypes.shape({
    id: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
    paused: PropTypes.bool.isRequired,
    name: PropTypes.string.isRequired,
    assetCounts: PropTypes.shape({
      assetCreatedCount: PropTypes.number.isRequired,
      assetReplacedCount: PropTypes.number.isRequired,
      assetWarningCount: PropTypes.number.isRequired,
      assetErrorCount: PropTypes.number.isRequired,
      assetTotalCount: PropTypes.number.isRequired,
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
