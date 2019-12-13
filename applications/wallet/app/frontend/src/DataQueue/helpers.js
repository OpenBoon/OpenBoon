export const getColumns = [
  'Status',
  'Job Name',
  'Created By',
  'Priority',
  'Created',
  'Failed',
  'Errors',
  '# Assets',
  'Progress',
]

export const getRows = ({ jobs }) =>
  jobs.map(job => {
    const { id } = job
    const status = job.paused ? 'Paused' : job.state
    const jobName = job.name
    const createdBy = job.createdUser.username
    const { priority } = job
    const createdDateTime = job.timeCreated

    const failed = job.taskCounts.tasksFailure > 0 && (
      <div style={{ color: 'red' }}>{job.taskCounts.tasksFailure}</div>
    )

    const errors = job.assetCounts.assetErrorCount > 0 && (
      <div style={{ color: 'red' }}>{job.assetCounts.assetErrorCount}</div>
    )

    const numAssets = 'numAsets'

    const progress = {
      isGenerating: job.jobId === '1585ca03-4db0-14d1-8edd-0a580a000926',
      isCanceled: job.state === 'Canceled',
      canceledBy: job.createdUser.username,
      failed: job.taskCounts.tasksFailure,
      pending: job.taskCounts.tasksWaiting,
      running: job.taskCounts.tasksRunning,
      succeeded: job.taskCounts.tasksSuccess,
    }

    return {
      id,
      status,
      jobName,
      createdBy,
      priority,
      createdDateTime,
      failed,
      errors,
      numAssets,
      progress,
    }
  })
