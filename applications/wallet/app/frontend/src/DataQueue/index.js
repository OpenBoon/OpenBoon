import PropTypes from 'prop-types'
import Head from 'next/head'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import PageTitle from '../PageTitle'
import Table from '../Table'
import Pagination from '../Pagination'

import DataQueueRow from './Row'

export const noop = () => () => {}

const SIZE = 3

const DataQueue = ({ selectedProject }) => {
  const router = useRouter()
  const {
    query: { page = 1 },
  } = router

  const parsedPage = parseInt(page, 10)
  const from = parsedPage * SIZE - SIZE

  const { data: { count = null, results } = {} } = useSWR(
    `/api/v1/projects/${selectedProject.id}/jobs/?from=${from}&size=${SIZE}`,
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
        renderRow={job => <DataQueueRow key={job.id} job={job} />}
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

DataQueue.propTypes = {
  selectedProject: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
  }).isRequired,
}

export default DataQueue
