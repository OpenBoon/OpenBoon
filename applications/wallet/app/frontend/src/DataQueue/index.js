import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Table from '../Table'

import DataQueueRow from './Row'

export const noop = () => () => {}

const DataQueue = () => {
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
        renderRow={({ result, revalidate }) => (
          <DataQueueRow
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

export default DataQueue
