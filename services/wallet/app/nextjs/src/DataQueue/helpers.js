import { useMemo } from 'react'

const newJob = ({ job }) => {
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

export const createJobsData = ({ jobs }) => {
  const dataRows = jobs.map(job => {
    return newJob({ job })
  })
  return dataRows
}

export const createColumns = ({ columnOptions }) => {
  return useMemo(() => columnOptions, []) // memoization required by react-table
}
