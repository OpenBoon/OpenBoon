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
    const {
      id,
      paused,
      state,
      name,
      createdUser,
      taskCounts,
      assetCounts,
      priority,
      timeCreated,
    } = job
    const status = paused ? 'Paused' : state
    const createdBy = createdUser.username
    const createdDateTime = timeCreated
    const failedTasks = taskCounts.tasksFailure
    const { assetErrorCount } = assetCounts
    const numAssets = 'numAsets'
    const progress = {
      isCanceled: state === 'Canceled',
      canceledBy: createdUser.username,
      failed: taskCounts.tasksFailure,
      pending: taskCounts.tasksWaiting,
      running: taskCounts.tasksRunning,
      succeeded: taskCounts.tasksSuccess,
    }

    return {
      id,
      status,
      name,
      createdBy,
      priority,
      createdDateTime,
      failedTasks,
      assetErrorCount,
      numAssets,
      progress,
    }
  })
