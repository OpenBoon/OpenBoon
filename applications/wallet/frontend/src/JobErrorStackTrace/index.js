import PropTypes from 'prop-types'

import JsonDisplay from '../JsonDisplay'

import { colors, spacing } from '../Styles'

const JobErrorStackTrace = ({ jobError: { message, stackTrace } }) => {
  return (
    <div
      css={{
        backgroundColor: colors.structure.black,
        fontFamily: 'Roboto Mono',
        padding: spacing.normal,
        height: 'auto',
      }}>
      <div>&quot;message&quot;: {message}</div>
      {!!stackTrace.length && (
        <>
          <div
            css={{
              paddingTop: spacing.normal,
              color: colors.structure.zinc,
              whiteSpace: 'nowrap',
            }}
          />
          <JsonDisplay json={stackTrace} />
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
