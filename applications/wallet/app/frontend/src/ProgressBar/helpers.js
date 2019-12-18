import { colors } from '../Styles'

export const TASK_STATUS_COLORS = {
  tasksFailure: colors.signal.warning.base,
  tasksSkipped: colors.structure.zinc,
  tasksSuccess: colors.signal.grass.base,
  tasksRunning: colors.signal.canary.base,
  tasksPending: colors.signal.sky.base,
}

export const getTimeEnded = ({ state, currentTime, timeUpdated }) => {
  const timeEnded = state === 'In Progress' ? currentTime : timeUpdated
  return timeEnded
}
