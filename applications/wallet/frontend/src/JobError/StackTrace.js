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
      <div>&quot;message&quot;: {message}</div>
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
              /*  eslint-disable react/no-array-index-key */
              <div key={index}>
                <div>{`{`}</div>
                <div
                  css={{
                    paddingLeft: spacing.large,
                    overflow: 'auto',
                  }}>
                  {Object.entries(frame).map(([key, value]) => {
                    return (
                      <div
                        key={key}
                        css={{
                          paddingTop: spacing.base,
                          whiteSpace: 'nowrap',
                        }}>
                        &quot;{key}&quot;: {value}
                      </div>
                    )
                  })}
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
