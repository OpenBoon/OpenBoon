import React, { useMemo } from 'react'
import { makeData } from './dummyData'
import Button from '../Button'
import ProgressBar from '../ProgressBar'
import Table from '../Table'

function DataQueue() {
  const columns = useMemo(() => [
    {
      Header: 'Status',
      accessor: 'status',
      Cell: ({ cell }) => { return <Button {...cell} /> }

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
    },
    {
      Header: 'Failed',
      accessor: 'failed',
    },
    {
      Header: 'Errors',
      accessor: 'errors',
    },
    {
      Header: '# Assets',
      accessor: 'numAssets',
    },
    {
      Header: 'Progress',
      accessor: 'progress',
      Cell: ({ cell }) => { return (<ProgressBar status={cell.value} />) }
    },
  ], [])

  const data = useMemo(() => makeData(20), [])

  return (
    <div className="DataQueue">
      <div className="DataQueue__container">
        <div className="DataQueue__title">{'DATA QUEUE'}</div>
        <div className="DataQueue__table">
          <Table columns={columns} data={data} />
        </div>
      </div>
    </div>
  )
}

export default DataQueue