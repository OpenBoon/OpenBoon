import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Table from '../Table'

import JobsEmpty from './Empty'
import JobsRow from './Row'

const Jobs = () => {
  const {
    query: { projectId },
  } = useRouter()

  return (
    <>
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
          '#Actions#',
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
    </>
  )
}

export default Jobs
