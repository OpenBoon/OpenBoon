import PropTypes from 'prop-types'
import { colors, constants } from '../Styles'

const CONTAINER_HEIGHT = 16
const CONTAINER_WIDTH = 200
const STATUS_COLORS = {
  succeeded: colors.green1,
  failed: colors.error,
  running: colors.blue1,
  pending: colors.grey6,
}

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
  const { isGenerating, isCanceled, canceledBy } = status

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
      css={{
        display: 'flex',
        height: CONTAINER_HEIGHT,
        width: CONTAINER_WIDTH,
      }}>
      {['succeeded', 'failed', 'running', 'pending'].map(statusName => {
        if (status[statusName] > 0) {
          return (
            <div
              css={getStatusStyles({
                status: status[statusName],
                statusColor: STATUS_COLORS[statusName],
              })}
            />
          )
        }
      })}
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
