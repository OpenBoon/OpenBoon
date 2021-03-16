export const ACTIONS = {
  InProgress: [
    {
      name: 'Pause',
      action: 'pause',
      confirmation: 'Pausing job.',
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
      confirmation: 'Restarting job.',
    },
  ],
  Success: [],
  Archived: [],
  Failure: [
    {
      name: 'Retry All Failures',
      action: 'retry_all_failures',
      confirmation: 'Retrying all failures.',
    },
  ],
  Paused: [
    {
      name: 'Resume',
      action: 'resume',
      confirmation: 'Resuming job.',
    },
  ],
  // Follwing statuses are here for backwards compatibility with ZVI.
  Active: [
    {
      name: 'Cancel',
      action: 'cancel',
      confirmation: 'Canceling job.',
    },
    {
      name: 'Restart',
      action: 'restart',
      confirmation: 'Restarting job.',
    },
    {
      name: 'Retry All Failures',
      action: 'retry_all_failures',
      confirmation: 'Retrying all failures.',
    },
  ],
  Finished: [],
}
