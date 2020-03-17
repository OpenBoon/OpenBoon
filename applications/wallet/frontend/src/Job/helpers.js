export const ACTIONS = {
  InProgress: [
    {
      name: 'Pause',
      action: 'pause',
    },
    {
      name: 'Cancel',
      action: 'cancel',
    },
  ],
  Cancelled: [
    {
      name: 'Restart',
      action: 'restart',
    },
  ],
  Success: [],
  Archived: [],
  Failure: [
    {
      name: 'Retry All Failures',
      action: 'retry_all_failures',
    },
  ],
  Paused: [
    {
      name: 'Resume',
      action: 'resume',
    },
  ],
  // Follwing statuses are here for backwards compatibility with ZVI.
  Active: [
    {
      name: 'Cancel',
      action: 'cancel',
    },
    {
      name: 'Restart',
      action: 'restart',
    },
    {
      name: 'Retry All Failures',
      action: 'retry_all_failures',
    },
  ],
  Finished: [],
}
