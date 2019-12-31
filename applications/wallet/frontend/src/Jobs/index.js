import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Table from '../Table'

import JobsEmpty from './Empty'
import JobsRow from './Row'

export const noop = () => () => {}

const Jobs = () => {
  const {
    query: { projectId },
  } = useRouter()

  return (
    <div>
      <Head>
        <title>Job Queue</title>
      </Head>

      <PageTitle>Job Queue</PageTitle>

      <Table
        url={`/api/v1/projects/${projectId}/jobs/`}
        columns={[
          'Status',
          'Job Name',
          'Created By',
          'Priority',
          'Created',
          '# Assets',
          'Errors',
          'Task Progress',
        ]}
        renderEmpty={<JobsEmpty />}
        renderRow={({ result, revalidate }) => (
          <JobsRow
            key={result.id}
            projectId={projectId}
            job={result}
            revalidate={revalidate}
          />
        )}
      />
    </div>
  )
}

export default Jobs
