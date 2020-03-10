import PropTypes from 'prop-types'
import JSONPretty from 'react-json-pretty'

import { colors, spacing } from '../Styles'

const JobErrorStackTrace = ({ jobError: { message, stackTrace } }) => {
  return (
    <div
      css={{
        backgroundColor: colors.structure.black,
        fontFamily: 'Roboto Mono',
        padding: spacing.normal,
        height: '100%',
      }}>
      <div>&quot;message&quot;: {message}</div>
      {stackTrace.length ? (
        <>
          <div
            css={{
              paddingTop: spacing.normal,
              color: colors.structure.zinc,
              whiteSpace: 'nowrap',
            }}
          />
          <JSONPretty
            id="json-pretty"
            data={stackTrace}
            theme={{
              main: 'line-height:1.3;overflow:auto;',
              string: `color:${colors.signal.grass.base};`,
              value: `color:${colors.signal.sky.base};`,
              boolean: `color:${colors.signal.canary.base};`,
            }}
          />
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
