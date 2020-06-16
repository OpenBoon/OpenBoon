import PropTypes from 'prop-types'

import JsonDisplay from '../JsonDisplay'

import { colors, spacing } from '../Styles'

const TaskErrorStackTrace = ({ taskError: { message, stackTrace } }) => {
  return (
    <div css={{ paddingBottom: spacing.spacious }}>
      <div
        css={{
          fontFamily: 'Roboto Mono',
          padding: spacing.normal,
          backgroundColor: colors.structure.coal,
          wordBreak: 'break-all',
        }}
      >
        &quot;message&quot;: {message}
      </div>

      {!!stackTrace.length && <JsonDisplay json={stackTrace} />}
    </div>
  )
}

TaskErrorStackTrace.propTypes = {
  taskError: PropTypes.shape({
    message: PropTypes.string.isRequired,
    stackTrace: PropTypes.arrayOf(
      PropTypes.shape({
        file: PropTypes.string.isRequired,
        lineNumber: PropTypes.number.isRequired,
        className: PropTypes.string.isRequired,
        methodName: PropTypes.string.isRequired,
      }),
    ).isRequired,
  }).isRequired,
}

export default TaskErrorStackTrace
