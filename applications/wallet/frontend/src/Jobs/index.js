import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Table, { ROLES } from '../Table'

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
        role={ROLES.ML_Tools}
        legend="Jobs"
        url={`/api/v1/projects/${projectId}/jobs/`}
        refreshKeys={[]}
        refreshButton
        columns={[
          { key: 'state', label: 'Status' },
          { key: 'name', label: 'Job Name' },
          { key: 'priority', label: 'Priority' },
          { key: 'timeCreated', label: 'Created' },
          '# Assets',
          'Errors',
          'Task Progress',
          '#Actions#',
        ]}
        expandColumn={2}
        renderEmpty={
          <>
            <div>There are currently no jobs in the queue.</div>
            <div>Any new job will appear here.</div>
          </>
        }
        renderRow={({ result, revalidate }) => (
          <JobsRow
            key={result.id}
            projectId={projectId}
            job={result}
            revalidate={revalidate}
          />
        )}
        searchLabel="Job Name"
        options={{ refreshInterval: 5000 }}
      />
    </>
  )
}

export default Jobs
