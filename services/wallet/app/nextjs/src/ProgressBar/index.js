import PropTypes from 'prop-types'
import { css } from '@emotion/core'
import { colors, spacing } from '../Styles'

const ProgressBar = ({ status }) => {
  const { succeeded, failed, running, pending } = status

  const containerCSS = css`
     {
      display: flex;
      height: ${spacing.normal}px;
      width: ${spacing.base * 25}px;
      :first-of-type {
        border-top-left-radius: ${spacing.base / 4}px;
        border-bottom-left-radius: ${spacing.base / 4}px;
      }
      :last-of-type {
        border-top-right-radius: ${spacing.base / 4}px;
        border-bottom-right-radius: ${spacing.base / 4}px;
      }
    }
  `

  function getStatusCSS(status, statusColor) {
    const statusCSS = css`
       {
        height: 100%;
        flex: ${status} 0 auto;
        background-color: ${statusColor};
        :first-of-type {
          border-top-left-radius: ${spacing.base / 4}px;
          border-bottom-left-radius: ${spacing.base / 4}px;
        }
        :last-of-type {
          border-top-right-radius: ${spacing.base / 4}px;
          border-bottom-right-radius: ${spacing.base / 4}px;
        }
      }
    `

    return statusCSS
  }

  return (
    <div className="ProgressBar__container" css={containerCSS}>
      {succeeded > 0 && (
        <div
          className="ProgressBar__Succeeded"
          css={getStatusCSS(succeeded, colors.green1)}
        />
      )}
      {failed > 0 && (
        <div
          className="ProgressBar__Failed"
          css={getStatusCSS(failed, colors.error)}
        />
      )}
      {running > 0 && (
        <div
          className="ProgressBar__Running"
          css={getStatusCSS(running, colors.blue1)}
        />
      )}
      {pending > 0 && (
        <div
          className="ProgressBar__Pending"
          css={getStatusCSS(pending, colors.grey6)}
        />
      )}
    </div>
  )
}

ProgressBar.propTypes = {
  status: PropTypes.shape({
    succeeded: PropTypes.number,
    failed: PropTypes.number,
    running: PropTypes.number,
    pending: PropTypes.number,
  }).isRequired,
}

export default ProgressBar
