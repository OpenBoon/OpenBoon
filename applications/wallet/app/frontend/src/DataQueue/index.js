import Head from 'next/head'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import PageTitle from '../PageTitle'
import Table from '../Table'
import Pagination from '../Pagination'

import DataQueueRow from './Row'
import DataQueueEmpty from './Empty'

export const noop = () => () => {}

const SIZE = 20

const DataQueue = () => {
  const {
    query: { projectId, page = 1 },
  } = useRouter()

  const parsedPage = parseInt(page, 10)
  const from = parsedPage * SIZE - SIZE

  const { data: { count = 0, results } = {}, revalidate } = useSWR(
    `/api/v1/projects/${projectId}/jobs/?from=${from}&size=${SIZE}`,
  )

  if (!Array.isArray(results)) return 'Loading...'

  if (results.length === 0) return 'You have 0 jobs'

  const to = Math.min(parsedPage * SIZE, count)

  return (
    <div>
      <Head>
        <title>Job Queue</title>
      </Head>

      <PageTitle>Job Queue</PageTitle>

      <Table
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
        items={results}
        renderEmpty={() => <DataQueueEmpty />}
        renderRow={job => (
          <DataQueueRow
            key={job.id}
            projectId={projectId}
            job={job}
            revalidate={revalidate}
          />
        )}
      />

      <div>&nbsp;</div>

      <Pagination
        legend={`Jobs: ${from + 1}–${to} of ${count}`}
        currentPage={parsedPage}
        totalPages={Math.ceil(count / SIZE)}
        prevLink={`/?page=${parsedPage - 1}`}
        nextLink={`/?page=${parsedPage + 1}`}
      />
    </div>
  )
}

export default DataQueue
