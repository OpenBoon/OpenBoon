import NoJobsSvg from '../Icons/noJobs.svg'

const DataQueueEmpty = () => {
  return (
    <div>
      <NoJobsSvg width={40} color="red" />
      <div>There are currently no jobs in the queue.</div>
      <div>Any new job will appear here.</div>
    </div>
  )
}

export default DataQueueEmpty
