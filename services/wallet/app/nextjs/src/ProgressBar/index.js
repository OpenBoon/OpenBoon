import PropTypes from 'prop-types'
import { colors, spacing, constants } from '../Styles'

const CONTAINER_WIDTH = 200

const getStatusStyles = ({ status, statusColor }) => {
  const statusCSS = {
    height: '100%',
    flex: `${status} 0 auto`,
    backgroundColor: statusColor,
    '&:first-of-type': {
      borderTopLeftRadius: constants.borderRadius.small,
      borderBottomLeftRadius: constants.borderRadius.small,
    },
    '&:last-of-type': {
      borderTopRightRadius: constants.borderRadius.small,
      borderBottomRightRadius: constants.borderRadius.small,
    },
  }

  return statusCSS
}

const ProgressBar = ({ status }) => {
  const {
    isGenerating,
    isCanceled,
    canceledBy,
    succeeded,
    failed,
    running,
    pending,
  } = status

  if (isGenerating) {
    return <div css={{ color: colors.blue1 }}>{'[ICON] Generating'}</div>
  }

  if (isCanceled) {
    return (
      <div css={{ color: colors.grey5 }}>{`Canceled by: ${canceledBy}`}</div>
    )
  }

  return (
    <div
      className="ProgressBar__container"
      css={{
        display: 'flex',
        height: spacing.normal,
        width: CONTAINER_WIDTH,
      }}>
      {succeeded > 0 && (
        <div
          className="ProgressBar__Succeeded"
          css={getStatusStyles({
            status: succeeded,
            statusColor: colors.green1,
          })}
        />
      )}
      {failed > 0 && (
        <div
          className="ProgressBar__Failed"
          css={getStatusStyles({ status: failed, statusColor: colors.error })}
        />
      )}
      {running > 0 && (
        <div
          className="ProgressBar__Running"
          css={getStatusStyles({ status: running, statusColor: colors.blue1 })}
        />
      )}
      {pending > 0 && (
        <div
          className="ProgressBar__Pending"
          css={getStatusStyles({ status: pending, statusColor: colors.grey6 })}
        />
      )}
    </div>
  )
}

ProgressBar.propTypes = {
  status: PropTypes.shape({
    isGenerating: PropTypes.bool,
    isCanceled: PropTypes.bool,
    canceledBy: PropTypes.string,
    succeeded: PropTypes.number,
    failed: PropTypes.number,
    running: PropTypes.number,
    pending: PropTypes.number,
  }).isRequired,
}

export default ProgressBar
