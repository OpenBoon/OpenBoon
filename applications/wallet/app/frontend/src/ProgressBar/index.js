import PropTypes from 'prop-types'
import { keyframes } from '@emotion/core'

import { STATUS_COLORS } from './helpers'
import InfoKey from './InfoKey'
import { colors, constants, spacing, zIndex } from '../Styles'
import GeneratingSvg from './generating.svg'

const CONTAINER_HEIGHT = 16
const CONTAINER_WIDTH = 212

const ProgressBar = ({ status, duration, showKeyInfo }) => {
  const { isGenerating } = status

  if (isGenerating) {
    const spinAnimation = keyframes`
      0% { transform: rotate(0deg) }
      100% { transform: rotate(360deg) }
    `
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

  return (
    <div
      css={{
        position: 'relative',
        display: 'flex',
        height: CONTAINER_HEIGHT,
        width: CONTAINER_WIDTH,
      }}>
      {['failed', 'skipped', 'succeeded', 'running', 'pending']
        .filter(statusName => {
          return status[statusName] > 0
        })
        .map(statusName => {
          return (
            <div
              key={statusName}
              css={{
                height: '100%',
                flex: `${status[statusName]} 0 auto`,
                backgroundColor: STATUS_COLORS[statusName],
                '&:first-of-type': {
                  borderTopLeftRadius: constants.borderRadius.small,
                  borderBottomLeftRadius: constants.borderRadius.small,
                },
                '&:last-of-type': {
                  borderTopRightRadius: constants.borderRadius.small,
                  borderBottomRightRadius: constants.borderRadius.small,
                },
              }}
            />
          )
        })}
      <div
        css={{
          position: 'absolute',
          top: CONTAINER_HEIGHT + spacing.base,
          right: 0,
          boxShadow: constants.boxShadows.tableRow,
          zIndex: zIndex.layout.dropdown,
        }}>
        {showKeyInfo && <InfoKey status={status} duration={duration} />}
      </div>
    </div>
  )
}

ProgressBar.propTypes = {
  status: PropTypes.shape({
    isGenerating: PropTypes.bool,
    isCanceled: PropTypes.bool,
    canceledBy: PropTypes.string,
    failed: PropTypes.number,
    skipped: PropTypes.number,
    succeeded: PropTypes.number,
    running: PropTypes.number,
    pending: PropTypes.number,
  }).isRequired,
  duration: PropTypes.shape({
    days: PropTypes.number,
    hours: PropTypes.number,
    minutes: PropTypes.number,
    seconds: PropTypes.number,
  }).isRequired,
  showKeyInfo: PropTypes.bool.isRequired,
}

export default ProgressBar
