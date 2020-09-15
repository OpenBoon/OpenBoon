import { useRef, useEffect } from 'react'
import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import { updatePlayheadPosition, GUIDE_WIDTH } from './helpers'

const TimelinePlayhead = ({ videoRef }) => {
  const playheadRef = useRef()
  const frameRef = useRef()

  const video = videoRef.current
  const playhead = playheadRef.current

  useEffect(() => {
    /* istanbul ignore next */
    const animate = () => {
      updatePlayheadPosition({ video, playhead })

      frameRef.current = requestAnimationFrame(animate)
    }

    frameRef.current = requestAnimationFrame(animate)

    return () => cancelAnimationFrame(frameRef.current)
  }, [video, playhead])

  return (
    <div>
      <div css={{ width: constants.timeline.modulesWidth }} />
      <div
        ref={playheadRef}
        css={{
          position: 'absolute',
          marginTop: 0,
          top:
            constants.timeline.rulerRowHeight -
            constants.timeline.playheadHeight,
          bottom: 0,
          left: 0,
          width: GUIDE_WIDTH,
          backgroundColor: colors.signal.sky.base,
        }}
      >
        <div
          css={{
            position: 'absolute',
            left: -((constants.timeline.playheadWidth - GUIDE_WIDTH) / 2),
            top: 0,
            width: '0',
            height: '0',
            borderStyle: 'solid',
            borderWidth: `${constants.timeline.playheadHeight}px ${
              constants.timeline.playheadWidth / 2
            }px 0 ${constants.timeline.playheadWidth / 2}px`,
            borderColor: `${colors.signal.sky.base} transparent transparent transparent`,
          }}
        />
      </div>
    </div>
  )
}

TimelinePlayhead.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      currentTime: PropTypes.number.isRequired,
      duration: PropTypes.number.isRequired,
    }),
  }).isRequired,
}

export default TimelinePlayhead
