import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Table, { ROLES } from '../Table'

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
        renderEmpty={<JobsEmpty />}
        renderRow={({ result, revalidate }) => (
          <JobsRow
            key={result.id}
            projectId={projectId}
            job={result}
            revalidate={revalidate}
          />
        )}
        searchLabel="Job Name"
      />
    </>
  )
}

export default Jobs
