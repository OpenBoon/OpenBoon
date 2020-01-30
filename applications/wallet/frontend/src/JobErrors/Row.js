import PropTypes from 'prop-types'

import { formatFullDate } from '../Date/helpers'

import JobErrorsMenu from './Menu'

const JobErrorsRow = ({
  projectId,
  jobId,
  error: {
    id: errorId,
    taskId,
    message,
    processor,
    fatal,
    timeCreated,
    stackTrace,
  },
  revalidate,
}) => {
  return (
    <tr>
      <td>{fatal ? 'Fatal' : 'Warning'}</td>
      <td>{errorId}</td>
      <td>{taskId}</td>
      <td>{message}</td>
      <td>{stackTrace[stackTrace.length - 1].file}</td>
      <td>{processor}</td>
      <td>{formatFullDate({ timestamp: timeCreated })}</td>
      <td>
        <JobErrorsMenu
          projectId={projectId}
          jobId={jobId}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

JobErrorsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
  error: PropTypes.shape({
    id: PropTypes.string.isRequired,
    taskId: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    processor: PropTypes.string.isRequired,
    fatal: PropTypes.bool.isRequired,
    timeCreated: PropTypes.number.isRequired,
    stackTrace: PropTypes.arrayOf(
      PropTypes.shape({
        file: PropTypes.string.isRequired,
      }).isRequired,
    ).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobErrorsRow
