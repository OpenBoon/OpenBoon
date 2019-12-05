import PropTypes from 'prop-types'
import useSWR from 'swr'
import Table from '../Table'
import { jobsColumns, jobsRows } from './__mocks__/jobs'

import { spacing } from '../Styles'

import Pagination from '../Pagination'

export const noop = () => () => {}

const Jobs = ({ logout }) => {
  const { data: { results = [] } = {} } = useSWR('/api/v1/projects/')

  return (
    <div css={{ padding: spacing.moderate }}>
      <h2>Projects</h2>
      {results.length === 0 ? (
        'Loading...'
      ) : (
        <ul>
          {results.map(({ name }) => (
            <li key={name}>{name}</li>
          ))}
        </ul>
      )}

      <Table columns={jobsColumns} rows={jobsRows} />

      <h2>Logout</h2>
      <button type="button" onClick={logout}>
        Logout
      </button>

      <h2>Pagination</h2>
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

Jobs.propTypes = {
  logout: PropTypes.func.isRequired,
}

export default Jobs
