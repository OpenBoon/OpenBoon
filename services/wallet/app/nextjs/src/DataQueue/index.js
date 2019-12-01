import PropTypes from 'prop-types'
import { useMemo } from 'react'
import { colors, spacing } from '../Styles'
import { ColumnStyle, createJobsData } from './helpers'
import { jobs } from './__mocks__/jobs'

import Button from '../Button'
import ProgressBar from '../ProgressBar'
import DateComponent from '../Date'
import Table from '../Table'

const DataQueue = () => {
  const columns = useMemo(
    () => [
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
          return DateComponent({ timeCreated: value })
        },
      },
      {
        Header: 'Failed',
        accessor: 'failed',
        Cell: ({ cell: { value } }) => {
          if (value === 0) {
            return ColumnStyle({ display: 'none' }, value)
          }
          return ColumnStyle({ color: 'red' }, value)
        },
      },
      {
        Header: 'Errors',
        accessor: 'errors',
        Cell: ({ cell: { value } }) => {
          if (value === 0) {
            return ColumnStyle({ display: 'none' }, value)
          }
          return ColumnStyle({ color: 'red' }, value)
        },
        width: 50,
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
    ],
    [],
  )

  const data = useMemo(() => createJobsData(jobs.list), [])

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
