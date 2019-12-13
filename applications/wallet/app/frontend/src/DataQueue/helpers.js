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

    const failed = taskCounts.tasksFailure > 0 && (
      <div style={{ color: 'red' }}>{taskCounts.tasksFailure}</div>
    )

    const errors = assetCounts.assetErrorCount > 0 && (
      <div style={{ color: 'red' }}>{assetCounts.assetErrorCount}</div>
    )

    const numAssets = 'numAsets'

    const progress = {
      isGenerating: false,
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
      failed,
      errors,
      numAssets,
      progress,
    }
  })
