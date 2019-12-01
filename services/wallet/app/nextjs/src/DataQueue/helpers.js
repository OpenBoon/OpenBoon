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
    failed: job.taskCounts.tasksFailure,
    errors: job.assetCounts.assetErrorCount,
    numAssets: 'numAsets',
    progress: {
      isGenerating: job.jobId === '1585ca03-4db0-14d1-8edd-0a580a000926',
      isCanceled: job.state === 'Canceled',
      canceledBy: job.createdUser.username,
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
