import { colors } from '../Styles'

export const TASK_STATUS_COLORS = {
  Failed: colors.signal.warning.base,
  Skipped: colors.structure.zinc,
  Succeeded: colors.signal.grass.base,
  Running: colors.signal.canary.base,
  Pending: colors.signal.sky.base,
}
