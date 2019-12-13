import PropTypes from 'prop-types'

import Jobs from '../Jobs'
import Table from '../Table'
import { getColumns, getRows } from './helpers'

import { spacing } from '../Styles'

import Pagination from '../Pagination'

export const noop = () => () => {}

const DataQueue = ({ selectedProject }) => {
  return (
    <div css={{ padding: spacing.normal }}>
      <h2>Data Queue</h2>

      <Jobs selectedProject={selectedProject}>
        {({ jobs }) => (
          <>
            <Table columns={getColumns} rows={getRows({ jobs })} />

            <div>&nbsp;</div>

            <Pagination
              legend="Jobs: 1–17 of 415"
              currentPage={1}
              totalPages={2}
              prevLink="/"
              nextLink="/?page=2"
              onClick={noop}
            />
          </>
        )}
      </Jobs>
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
