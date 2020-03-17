import { useRouter } from 'next/router'

import Table from '../Table'

import JobErrorsEmpty from './Empty'
import JobErrorsRow from './Row'

const JobErrors = () => {
  const {
    query: { projectId, jobId },
  } = useRouter()

  return (
    <Table
      assetType="Errors"
      url={`/api/v1/projects/${projectId}/jobs/${jobId}/errors/`}
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
