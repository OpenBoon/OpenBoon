import PropTypes from 'prop-types'
import useSWR from 'swr'
import Table from '../Table'
import { jobsColumns, jobsRows } from './__mocks__/jobs'

const Jobs = ({ logout }) => {
  const { data: { results = [] } = {} } = useSWR('/api/v1/projects/')

  return (
    <div>
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
    </div>
  )
}

Jobs.propTypes = {
  logout: PropTypes.func.isRequired,
}

export default Jobs
