import { useRouter } from 'next/router'

import Table, { ROLES } from '../Table'

import JobTasksEmpty from './Empty'
import JobTasksRow from './Row'

const JobTasks = () => {
  const {
    query: { projectId, jobId },
  } = useRouter()

  return (
    <Table
      role={ROLES.ML_Tools}
      legend="Tasks"
      url={`/api/v1/projects/${projectId}/jobs/${jobId}/tasks/`}
      refreshKeys={[`/api/v1/projects/${projectId}/jobs/${jobId}/`]}
      columns={[
        'Task State',
        'Task ID',
        'Description',
        'Started',
        'Duration',
        'Errors',
      ]}
      expandColumn={3}
      renderEmpty={<JobTasksEmpty />}
      renderRow={({ result }) => (
        <JobTasksRow
          key={result.id}
          projectId={projectId}
          jobId={jobId}
          task={result}
        />
      )}
    />
  )
}

export default JobTasks
