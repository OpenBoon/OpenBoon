export const ColumnStyle = (style, value) => {
  return <div style={style}>{value}</div>
}

const newJob = job => {
  return {
    status: job.paused ? 'Paused' : job.state,
    jobName: job.name,
    createdBy: job.createdUser.username,
    priority: job.priority,
    createdDateTime: job.timeCreated,
    failed: '??',
    errors: job.assetCounts.assetErrorCount,
    numAssets: 'numAsets',
    progress: {
      failed: job.taskCounts.tasksFailure,
      pending: job.taskCounts.tasksWaiting,
      running: job.taskCounts.tasksRunning,
      succeeded: job.taskCounts.tasksSuccess,
    },
  }
}

export function createJobsData(jobsArray) {
  const dataRows = jobsArray.map(job => {
    return newJob(job)
  })
  return dataRows
}
