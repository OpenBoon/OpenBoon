import PropTypes from 'prop-types'
import Head from 'next/head'
import useSWR from 'swr'

import PageTitle from '../PageTitle'
import Table from '../Table'
import Pagination from '../Pagination'

import DataQueueRow from './Row'

export const noop = () => () => {}

const DataQueue = ({ selectedProject }) => {
  const { data: { results } = {} } = useSWR(
    `/api/v1/projects/${selectedProject.id}/jobs/`,
  )

  if (!Array.isArray(results)) return 'Loading...'

  if (results.length === 0) return 'You have 0 jobs'

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
        legend="Jobs: 1–17 of 415"
        currentPage={1}
        totalPages={2}
        prevLink="/"
        nextLink="/?page=2"
        onClick={noop}
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
