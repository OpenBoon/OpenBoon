import PropTypes from 'prop-types'
import Table from '../Table'
import { jobColumns, jobRows } from './__mocks__/jobs'

import { spacing } from '../Styles'

import Pagination from '../Pagination'
import UserMenu from '../UserMenu'

export const noop = () => () => {}

const Jobs = ({ logout }) => {
  return (
    <div css={{ padding: spacing.moderate }}>
      <Table columns={jobColumns} rows={jobRows} />

      <h2>UserMenu</h2>
      <div css={{ display: 'flex', justifyContent: 'flex-end' }}>
        <UserMenu logout={logout} />
      </div>

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
