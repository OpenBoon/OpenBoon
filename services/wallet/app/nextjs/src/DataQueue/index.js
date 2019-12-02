import PropTypes from 'prop-types'
import { useMemo } from 'react'
import { colors, spacing } from '../Styles'
import { createJobsData, createColumns } from './helpers'
import { jobs } from './__mocks__/jobs'

import Button from '../Button'
import ProgressBar from '../ProgressBar'
import FormattedDate from '../Format/Date'
import Table from '../Table'
import FormattedColumn from '../Format/Column'

const COLUMN_OPTIONS = [
  {
    Header: 'Status',
    accessor: 'status',
    Cell: ({ cell: { value } }) => {
      return Button({ status: value })
    },
  },
  {
    Header: 'Job Name',
    accessor: 'jobName',
  },
  {
    Header: 'Created By',
    accessor: 'createdBy',
  },
  {
    Header: 'Priority',
    accessor: 'priority',
  },
  {
    Header: 'Created (Date/TIme)',
    accessor: 'createdDateTime',
    Cell: ({ cell: { value } }) => {
      return FormattedDate({ timeCreated: value })
    },
  },
  {
    Header: 'Failed',
    accessor: 'failed',
    Cell: ({ cell: { value } }) => {
      if (value === 0) {
        return FormattedColumn({ display: 'none' }, { content: value })
      }
      return FormattedColumn({ color: 'red' }, { content: value })
    },
  },
  {
    Header: 'Errors',
    accessor: 'errors',
    Cell: ({ cell: { value } }) => {
      if (value === 0) {
        return FormattedColumn({ display: 'none' }, { content: value })
      }
      return FormattedColumn({ color: 'red' }, { content: value })
    },
  },
  {
    Header: '# Assets',
    accessor: 'numAssets',
  },
  {
    Header: 'Progress',
    accessor: 'progress',
    Cell: ({ cell: { value } }) => {
      return ProgressBar({ status: value })
    },
  },
]

const DataQueue = () => {
  const columns = createColumns({
    columnOptions: COLUMN_OPTIONS,
  })

  const data = useMemo(() => createJobsData({ jobs: jobs.list }), [])

  return (
    <div
      className="DataQueue"
      css={{
        height: '100%',
        width: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        padding: `${spacing.moderate * 7}px ${spacing.spacious}px`,
      }}>
      <div
        className="DataQueue__container"
        css={{
          display: 'flex',
          flexDirection: 'column',
          width: '100%',
          height: '100%',
        }}>
        <div
          className="DataQueue__title"
          css={{
            color: colors.grey2,
          }}>
          {'DATA QUEUE'}
        </div>
        <div className="DataQueue__table">
          <Table columns={columns} data={data} />
        </div>
      </div>
    </div>
  )
}

DataQueue.propTypes = {
  cell: PropTypes.shape({
    value: PropTypes.string,
  }),
}

DataQueue.defaultProps = {
  cell: {},
}

export default DataQueue
