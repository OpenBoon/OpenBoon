import { useMemo } from 'react'
import { colors, spacing } from '../Styles'
import { createJobsData } from './helpers'
import { formatFullDate } from '../Format/Date/helpers'
import { jobs } from './__mocks__/jobs'

import Button from '../Button'
import ProgressBar from '../ProgressBar'
import Table from '../Table'
import FormattedColumn from '../Format/Column'

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
          return String(formatFullDate({ timestamp: value }))
        },
      },
      {
        Header: 'Failed',
        accessor: 'failed',
        Cell: ({ cell: { value } }) => {
          if (value === 0) {
            return FormattedColumn({
              style: { display: 'none' },
              content: value,
            })
          }
          return FormattedColumn({ style: { color: 'red' }, content: value })
        },
      },
      {
        Header: 'Errors',
        accessor: 'errors',
        Cell: ({ cell: { value } }) => {
          if (value === 0) {
            return FormattedColumn({
              style: { display: 'none' },
              content: value,
            })
          }
          return FormattedColumn({ style: { color: 'red' }, content: value })
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
    ],
    [],
  )

  const data = useMemo(() => createJobsData({ jobs: jobs.list }), [])

  return (
    <div
      css={{
        height: '100%',
        width: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        padding: `${spacing.enormous}px ${spacing.spacious}px`,
      }}>
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          width: '100%',
          height: '100%',
        }}>
        <div
          css={{
            color: colors.grey2,
          }}>
          {'DATA QUEUE'}
        </div>
        <Table columns={columns} data={data} />
      </div>
    </div>
  )
}

export default DataQueue
