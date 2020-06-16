import PropTypes from 'prop-types'

import JsonDisplay from '../JsonDisplay'

import { colors, spacing } from '../Styles'

const TaskErrorStackTrace = ({ taskError: { message, stackTrace } }) => {
  return (
    <div
      css={{
        fontFamily: 'Roboto Mono',
        paddingBottom: spacing.spacious,
        height: 'auto',
      }}
    >
      <div
        css={{
          backgroundColor: colors.structure.coal,
          padding: spacing.normal,
        }}
      >
        <div>&quot;message&quot;: {message}</div>

        {!!stackTrace.length && (
          <>
            <div
              css={{
                paddingTop: spacing.normal,
                color: colors.structure.zinc,
              }}
            />

            <JsonDisplay json={stackTrace} />
          </>
        )}
      </div>
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
