import Table from '../Table'
import { jobColumns, __mockJobRows } from './__mocks__/jobs'

import { spacing } from '../Styles'

import Pagination from '../Pagination'
import Modal from '../Modal'

export const noop = () => () => {}

const Jobs = () => {
  return (
    <div css={{ padding: spacing.spacious }}>
      <h2>Data Queue</h2>

      <Table columns={jobColumns} rows={__mockJobRows} />

      <div>&nbsp;</div>

      <Pagination
        legend="Jobs: 1–17 of 415"
        currentPage={1}
        totalPages={2}
        prevLink="/"
        nextLink="/?page=2"
        onClick={noop}
      />

      <Modal />
    </div>
  )
}

export default Jobs
