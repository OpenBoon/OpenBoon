import PropTypes from 'prop-types'
import { css } from '@emotion/core'
import { colors, spacing, constants } from '../Styles'

const CONTAINER_WIDTH = 200

const getStatusStyles = ({ status, statusColor }) => {
  const statusCSS = css`
     {
      height: 100%;
      flex: ${status} 0 auto;
      background-color: ${statusColor};
      :first-of-type {
        border-top-left-radius: ${constants.borderRadius.small}px;
        border-bottom-left-radius: ${constants.borderRadius.small}px;
      }
      :last-of-type {
        border-top-right-radius: ${constants.borderRadius.small}px;
        border-bottom-right-radius: ${constants.borderRadius.small}px;
      }
    }
  `

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

  const containerCSS = css`
     {
      display: flex;
      height: ${spacing.normal}px;
      width: ${CONTAINER_WIDTH}px;
      :first-of-type {
        border-top-left-radius: ${constants.borderRadius.small}px;
        border-bottom-left-radius: ${constants.borderRadius.small}px;
      }
      :last-of-type {
        border-top-right-radius: ${constants.borderRadius.small}px;
        border-bottom-right-radius: ${constants.borderRadius.small}px;
      }
    }
  `

  return (
    <div className="ProgressBar__container" css={containerCSS}>
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
