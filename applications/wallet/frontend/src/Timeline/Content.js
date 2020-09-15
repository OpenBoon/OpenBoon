import PropTypes from 'prop-types'

import { colors, constants, typography } from '../Styles'

import TimelinePlayhead from './Playhead'
import TimelineDetections from './Detections'

const TimelineControls = ({ isOpen, size, originSize, videoRef }) => {
  return (
    <div css={{ height: size, overflow: 'auto' }}>
      {isOpen && (
        <div
          css={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            height: '0%',
            position: 'relative',
            marginLeft: constants.timeline.modulesWidth,
            borderLeft: constants.borders.regular.smoke,
          }}
        >
          <TimelinePlayhead videoRef={videoRef} />

          {/* TODO: add ruler and other stuff here */}
          <div css={{ height: constants.timeline.rulerRowHeight }} />

          <TimelineDetections videoRef={videoRef} />
        </div>
      )}

      {!isOpen && (
        <div
          css={{
            height: '100%',
            width: '100%',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            fontSize: typography.size.medium,
            fontHeight: typography.height.medium,
            fontWeight: typography.weight.medium,
            overflow: 'hidden',
            color: colors.structure.steel,
          }}
        >
          Release to {size < originSize ? 'collapse' : 'expand'}.
        </div>
      )}
    </div>
  )
}

TimelineControls.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  size: PropTypes.number.isRequired,
  originSize: PropTypes.number.isRequired,
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      play: PropTypes.func,
      pause: PropTypes.func,
      addEventListener: PropTypes.func,
      removeEventListener: PropTypes.func,
      currentTime: PropTypes.number,
      duration: PropTypes.number,
      paused: PropTypes.bool,
    }),
  }).isRequired,
}

export default TimelineControls
