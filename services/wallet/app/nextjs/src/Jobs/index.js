import PropTypes from 'prop-types'
import { useMemo } from 'react'
import Button from '../Button'
import ProgressBar from '../ProgressBar'
import { colors, spacing } from '../Styles'
import DateComponent from '../Date'
import Table from '../Table'
import { ColumnStyle, createJobsData } from './helpers'
// import { makeData } from './__mocks__/dummyData'
import { jobs } from './__mocks__/jobs'

const Jobs = () => {
  const columns = useMemo(
    () => [
      {
        Header: 'Status',
        accessor: 'status',
        Cell: ({ cell }) => {
          return Button({ status: cell.value })
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
        Cell: ({ cell }) => {
          return DateComponent({ date: cell.value })
        },
      },
      {
        Header: 'Failed',
        accessor: 'failed',
        Cell: ({ cell }) => {
          return ColumnStyle({ color: 'red' }, cell.value)
        },
      },
      {
        Header: 'Errors',
        accessor: 'errors',
        Cell: ({ cell }) => {
          return ColumnStyle({ color: 'red' }, cell.value)
        },
      },
      {
        Header: '# Assets',
        accessor: 'numAssets',
      },
      {
        Header: 'Progress',
        accessor: 'progress',
        Cell: ({ cell }) => {
          return ProgressBar({ status: cell.value })
        },
      },
    ],
    [],
  )

  const data = useMemo(() => createJobsData(jobs.list), [])
  // const data = useMemo(() => makeData(20), [])

  return (
    <div
      className="DataQueue"
      css={{
        height: '100%',
        width: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
      }}>
      <div className="DataQueue__container">
        <div
          className="DataQueue__title"
          css={{
            color: colors.grey2,
            marginBottom: spacing.base,
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

Jobs.propTypes = {
  cell: PropTypes.shape({
    value: PropTypes.string,
  }),
}

Jobs.defaultProps = {
  cell: {},
}

export default Jobs
