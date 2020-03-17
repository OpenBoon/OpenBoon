import { useRouter } from 'next/router'

import Table from '../Table'

import JobTasksEmpty from './Empty'
import JobTasksRow from './Row'

const JobTasks = () => {
  const {
    query: { projectId, jobId },
  } = useRouter()

  return (
    <Table
      assetType="Tasks"
      url={`/api/v1/projects/${projectId}/jobs/${jobId}/tasks`}
      columns={[
        'Task State',
        'Task ID',
        'Description',
        'Duration',
        'Assets',
        'Updated',
        'Errors',
      ]}
      expandColumn={3}
      renderEmpty={<JobTasksEmpty />}
      renderRow={({ result }) => <JobTasksRow key={result.id} task={result} />}
    />
  )
}

export default JobTasks
