import PropTypes from 'prop-types'
import { keyframes } from '@emotion/core'
import { colors, constants, spacing } from '../Styles'
import GeneratingSvg from './generating.svg'

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
    const spinAnimation = keyframes`
      0% { transform: rotate(0deg) }
      100% { transform: rotate(360deg) }
    }`
    return (
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          color: colors.blue1,
        }}>
        <GeneratingSvg
          css={{
            color: colors.blue1,
            animation: `${spinAnimation} 2s linear infinite`,
          }}
        />
        <div css={{ paddingLeft: spacing.base }}>Generating</div>
      </div>
    )
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
              key={statusName}
              css={getStatusStyles({
                status: status[statusName],
                statusColor: STATUS_COLORS[statusName],
              })}
            />
          )
        }
        return ''
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
