import PropTypes from 'prop-types'

import ErrorFatalSvg from '../Icons/errorFatal.svg'
import ErrorWarningSvg from '../Icons/errorWarning.svg'

import { colors, spacing } from '../Styles'

const JobErrorType = ({ error: { message, fatal } }) => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        paddingBottom: spacing.comfy,
      }}>
      <div
        css={{
          display: 'flex',
          paddingBottom: spacing.base,
        }}>
        <div css={{ paddingRight: spacing.base }}>
          {fatal ? (
            <ErrorFatalSvg width={18} color={colors.signal.warning.base} />
          ) : (
            <ErrorWarningSvg width={18} color={colors.signal.canary.strong} />
          )}
        </div>
        <div
          css={{
            color: fatal
              ? colors.signal.warning.base
              : colors.signal.canary.strong,
            fontWeight: 700,
          }}>
          Error Type: {fatal ? 'Fatal' : 'Warning'}
        </div>
      </div>
      <div
        css={{
          fontWeight: 700,
          paddingRight: spacing.base,
          whiteSpace: 'noWrap',
          paddingBottom: spacing.small,
        }}>
        Error Message:
      </div>
      {message}
    </div>
  )
}

JobErrorType.propTypes = {
  error: PropTypes.shape({
    message: PropTypes.string.isRequired,
    fatal: PropTypes.bool.isRequired,
  }).isRequired,
}

export default JobErrorType
