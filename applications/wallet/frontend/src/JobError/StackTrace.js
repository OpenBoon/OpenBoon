import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

const JobErrorStackTrace = ({ jobError: { message, stackTrace } }) => {
  return (
    <div
      css={{
        backgroundColor: colors.structure.black,
        fontFamily: 'Roboto Mono',
        padding: spacing.normal,
      }}>
      <div>&quot;message&quot; : {message}</div>
      {stackTrace.length && (
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
                <div
                  css={{
                    paddingLeft: spacing.large,
                    overflow: 'auto',
                  }}>
                  {Object.keys(frame).map(line => {
                    return (
                      <div
                        key={line}
                        css={{
                          paddingTop: spacing.base,
                          whiteSpace: 'nowrap',
                        }}>
                        &quot;{line}&quot;: {frame[line]}
                      </div>
                    )
                  })}
                </div>
                <div>{index === stackTrace.length - 1 ? `}` : `},`}</div>
              </div>
            )
          })}
        </>
      )}
    </div>
  )
}

JobErrorStackTrace.propTypes = {
  jobError: PropTypes.shape({
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

export default JobErrorStackTrace
