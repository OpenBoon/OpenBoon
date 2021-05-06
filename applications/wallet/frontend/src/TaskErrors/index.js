import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import Table, { ROLES } from '../Table'

import TaskErrorsRow from './Row'

const TaskErrors = ({ parentUrl }) => {
  const {
    query: { projectId, jobId },
  } = useRouter()

  return (
    <Table
      role={ROLES.ML_Tools}
      legend="Errors"
      url={`${parentUrl}errors/`}
      refreshKeys={[parentUrl]}
      refreshButton={false}
      columns={[
        'Error Type',
        'Phase',
        'Message',
        'File Name',
        'Processor',
        'Time',
        '#Actions#',
      ]}
      expandColumn={0}
      renderEmpty={
        <>
          <div>This job has no errors.</div>
          <div>Any new error will appear here.</div>
        </>
      }
      renderRow={({ result, revalidate }) => (
        <TaskErrorsRow
          key={result.id}
          projectId={projectId}
          jobId={jobId}
          error={result}
          revalidate={revalidate}
        />
      )}
    />
  )
}

TaskErrors.propTypes = {
  parentUrl: PropTypes.string.isRequired,
}

export default TaskErrors
