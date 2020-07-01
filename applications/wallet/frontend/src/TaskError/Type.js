import PropTypes from 'prop-types'

import ErrorFatalSvg from '../Icons/errorFatal.svg'
import ErrorWarningSvg from '../Icons/errorWarning.svg'

import { colors, spacing, typography } from '../Styles'

const ICON_SIZE = 18

const TaskErrorType = ({ fatal }) => {
  return (
    <div
      css={{
        display: 'flex',
        paddingTop: spacing.spacious,
      }}
    >
      <div css={{ paddingRight: spacing.base }}>
        {fatal ? (
          <ErrorFatalSvg
            height={ICON_SIZE}
            color={colors.signal.warning.base}
          />
        ) : (
          <ErrorWarningSvg
            height={ICON_SIZE}
            color={colors.signal.canary.strong}
          />
        )}
      </div>
      <div
        css={{
          color: fatal
            ? colors.signal.warning.base
            : colors.signal.canary.strong,
          fontWeight: typography.weight.bold,
        }}
      >
        Error Type: {fatal ? 'Fatal' : 'Warning'}
      </div>
    </div>
  )
}

TaskErrorType.propTypes = {
  fatal: PropTypes.bool.isRequired,
}

export default TaskErrorType
