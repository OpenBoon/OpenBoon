const range = len => {
  const arr = []
  for (let i = 0; i < len; i++) {
    arr.push(i)
  }
  return arr
}

function generateStatus() {
  const statuses = ['Active', 'Paused', 'Canceled', 'Finished']
  const randomIndex = Math.floor(Math.random() * statuses.length)
  const randomStatus = statuses[randomIndex]
  return randomStatus
}

function generateProgress() {
  const status = {
    succeeded: Math.floor(Math.random() * 100),
    failed: Math.floor(Math.random() * 100),
    running: Math.floor(Math.random() * 100),
    pending: Math.floor(Math.random() * 100),
  }

  return status
}

const newJob = () => {
  return {
    status: generateStatus(),
    jobName: 'FooBar',
    createdBy: 'userX',
    priority: Math.floor(Math.random() * 100),
    createdDateTime: new Date(),
    failed: 'failed',
    errors: 'errors',
    numAssets: 'numAsets',
    progress: generateProgress(),
  }
}

export function makeData(...lens) {
  const makeDataLevel = (depth = 0) => {
    const len = lens[depth]
    return range(len).map(() => {
      return {
        ...newJob(),
        subRows: lens[depth + 1] ? makeDataLevel(depth + 1) : undefined,
      }
    })
  }

  return makeDataLevel()
}
