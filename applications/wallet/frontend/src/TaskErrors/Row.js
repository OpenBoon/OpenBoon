import PropTypes from 'prop-types'
import Router from 'next/router'
import Link from 'next/link'

import { formatFullDate } from '../Date/helpers'

import ErrorFatalSvg from '../Icons/errorFatal.svg'
import ErrorWarningSvg from '../Icons/errorWarning.svg'

import TaskErrorsMenu from './Menu'
import { colors, spacing, typography } from '../Styles'

const ICON_SIZE = 18

const TaskErrorsRow = ({
  projectId,
  jobId,
  error: {
    id: errorId,
    taskId,
    path,
    message,
    processor,
    fatal,
    phase,
    timeCreated,
  },
  revalidate,
}) => {
  return (
    <tr
      css={{ cursor: 'pointer' }}
      onClick={(event) => {
        const { target: { localName } = {} } = event || {}
        if (['a', 'button', 'svg', 'path'].includes(localName)) return
        Router.push(
          '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]',
          `/${projectId}/jobs/${jobId}/tasks/${taskId}/errors/${errorId}`,
        )
      }}
    >
      <td>
        <div
          css={{
            display: 'flex',
            aligntItems: 'center',
            justifyContent: 'flex-start',
            paddingTop: spacing.base,
            paddingBottom: spacing.base,
            paddingRight: spacing.base,
            span: {
              paddingLeft: spacing.moderate,
              fontWeight: typography.weight.medium,
            },
          }}
        >
          {fatal ? (
            <>
              <ErrorFatalSvg
                height={ICON_SIZE}
                color={colors.signal.warning.base}
              />
              <span>Fatal</span>
            </>
          ) : (
            <>
              <ErrorWarningSvg
                height={ICON_SIZE}
                color={colors.signal.canary.strong}
              />
              <span>Warning</span>
            </>
          )}
        </div>
      </td>
      <td>{phase}</td>
      <td style={{ width: '55%' }}>
        <Link
          href="/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]"
          as={`/${projectId}/jobs/${jobId}/tasks/${taskId}/errors/${errorId}`}
          passHref
        >
          <a css={{ ':hover': { textDecoration: 'none' } }} title={message}>
            {message}
          </a>
        </Link>
      </td>
      <td style={{ width: '50%' }} title={path}>
        {path}
      </td>
      <td title={processor}>{processor}</td>
      <td>{formatFullDate({ timestamp: timeCreated })}</td>
      <td>
        <TaskErrorsMenu
          projectId={projectId}
          taskId={taskId}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

TaskErrorsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
  error: PropTypes.shape({
    id: PropTypes.string.isRequired,
    taskId: PropTypes.string.isRequired,
    path: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    processor: PropTypes.string.isRequired,
    fatal: PropTypes.bool.isRequired,
    phase: PropTypes.string.isRequired,
    timeCreated: PropTypes.number.isRequired,
    stackTrace: PropTypes.arrayOf(
      PropTypes.shape({
        file: PropTypes.string.isRequired,
      }).isRequired,
    ).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default TaskErrorsRow
