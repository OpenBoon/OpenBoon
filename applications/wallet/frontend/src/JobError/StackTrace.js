import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

const JobErrorStackTrace = ({ jobError: { message, stackTrace } }) => {
  return (
    <div
      css={{
        backgroundColor: colors.structure.black,
        fontFamily: 'Roboto Mono',
        fontSize: typography.size.small,
        lineHeight: typography.height.small,
        padding: spacing.normal,
      }}>
      <div>{`"message" : ${message}`}</div>
      {stackTrace.length ? (
        <>
          <div
            css={{
              paddingTop: spacing.normal,
              color: colors.structure.zinc,
              whiteSpace: 'nowrap',
              overFlow: 'auto',
            }}
          />
          {stackTrace.map((frame, index) => {
            return (
              <div key={JSON.stringify(frame)}>
                <div>{`{`}</div>
                <div css={{ paddingLeft: spacing.large }}>
                  <div
                    css={{
                      paddingTop: spacing.base,
                    }}>{`"file": ${JSON.stringify(frame.file)}`}</div>
                  <div
                    css={{
                      paddingTop: spacing.base,
                    }}>{`"lineNumber": ${JSON.stringify(
                    frame.lineNumber,
                  )}`}</div>
                  <div
                    css={{
                      paddingTop: spacing.base,
                    }}>{`"className": ${JSON.stringify(frame.className)}`}</div>
                  <div
                    css={{
                      paddingTop: spacing.base,
                    }}>{`"methodName": ${JSON.stringify(
                    frame.methodName,
                  )}`}</div>
                </div>
                <div>{index === stackTrace.length - 1 ? `}` : `},`}</div>
              </div>
            )
          })}
        </>
      ) : null}
    </div>
  )
}

JobErrorStackTrace.propTypes = {
  jobError: PropTypes.shape({
    message: PropTypes.string.isRequired,
    stackTrace: PropTypes.arrayOf(PropTypes.object).isRequired,
  }).isRequired,
}

export default JobErrorStackTrace
