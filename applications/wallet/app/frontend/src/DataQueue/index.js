import { useState } from 'react'
import PropTypes from 'prop-types'
import Head from 'next/head'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import PageTitle from '../PageTitle'
import Table from '../Table'
import Pagination from '../Pagination'

export const noop = () => () => {}

const COLUMNS = [
  'Status',
  'Job Name',
  'Created By',
  'Priority',
  'Created',
  'Failed',
  'Errors',
  '# Assets',
  'Progress',
]

const SIZE = 3

const DataQueue = ({ selectedProject }) => {
  const router = useRouter()
  const {
    query: { page },
  } = router
  const [paginationParams, setPaginationParams] = useState({
    currentPage: parseInt(page, 10) || 1,
  })

  const { currentPage } = paginationParams
  const from = currentPage * SIZE - SIZE

  const {
    data: { count = null, previous = null, next = null, results } = {},
  } = useSWR(
    `/api/v1/projects/${selectedProject.id}/jobs/?from=${from}&size=${SIZE}`,
  )

  if (!Array.isArray(results)) return 'Loading...'

  if (results.length === 0) return 'You have 0 jobs'

  const to = Math.min(currentPage * SIZE, count)

  return (
    <div>
      <Head>
        <title>Data Queue</title>
      </Head>

      <PageTitle>Data Queue</PageTitle>

      <Table columns={COLUMNS} rows={results} />

      <div>&nbsp;</div>

      <Pagination
        legend={`Jobs: ${from + 1}–${to} of ${count}`}
        currentPage={currentPage}
        totalPages={Math.ceil(count / SIZE)}
        prevLink={previous ? `/?page=${currentPage - 1}` : '/'}
        nextLink={next ? `/?page=${currentPage + 1}` : `/`}
        onClick={({ newPage }) =>
          setPaginationParams({
            currentPage: newPage,
          })
        }
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
