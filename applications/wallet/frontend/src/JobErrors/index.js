import { useRouter } from 'next/router'

import Table, { ROLES } from '../Table'

import JobErrorsEmpty from './Empty'
import JobErrorsRow from './Row'

const JobErrors = () => {
  const {
    query: { projectId, jobId },
  } = useRouter()

  return (
    <Table
      role={ROLES.ML_Tools}
      legend="Errors"
      url={`/api/v1/projects/${projectId}/jobs/${jobId}/errors`}
      refreshKeys={[`/api/v1/projects/${projectId}/jobs/${jobId}`]}
      columns={[
        'Error Type',
        'Phase',
        'Message',
        'File Name',
        'Processor',
        'Time',
        '#Actions#',
      ]}
      expandColumn={0}
      renderEmpty={<JobErrorsEmpty />}
      renderRow={({ result, revalidate }) => (
        <JobErrorsRow
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

export default JobErrors
