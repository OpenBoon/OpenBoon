import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import PageTitle from '../PageTitle'
import Table from '../Table'
import Refresh from '../Refresh'

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

      <div css={{ paddingTop: spacing.comfy, paddingBottom: spacing.normal }}>
        <Refresh />
      </div>

      <Table
        url={`/api/v1/projects/${projectId}/jobs/`}
        columns={[
          'Status',
          'Job Name',
          'Priority',
          'Created',
          '# Assets',
          'Errors',
          'Task Progress',
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
      />
    </>
  )
}

export default Jobs
