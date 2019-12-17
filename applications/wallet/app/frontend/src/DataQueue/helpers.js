export const parsedRows = ({ jobs }) => {
  return jobs.map(job => {
    const {
      id,
      state,
      name,
      createdUser: { username },
      taskCounts: {
        tasksFailure,
        tasksSkipped,
        tasksSuccess,
        tasksRunning,
        tasksWaiting,
        tasksQueued,
      },
      assetCounts,
      priority,
      timeCreated,
      timeStarted,
    } = job
    const tasksProgress = {
      Failed: tasksFailure,
      Skipped: tasksSkipped,
      Succeeded: tasksSuccess,
      Running: tasksRunning,
      Pending: tasksWaiting + tasksQueued,
    }

    return {
      id,
      state,
      name,
      username,
      assetCounts,
      priority,
      timeCreated,
      timeStarted,
      tasksProgress,
    }
  })
}
