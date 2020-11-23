import { useCallback, useEffect } from 'react'
import PropTypes from 'prop-types'

import { gotoNextHit, gotoPreviousHit } from './helpers'

const TimelineShortcuts = ({
  videoRef,
  timelines,
  settings,
  setFollowPlayhead,
}) => {
  /* istanbul ignore next */
  const keydownHandler = useCallback(
    (event) => {
      const {
        code,
        shiftKey,
        metaKey,
        ctrlKey,
        target: { tagName },
      } = event
      const hasModifier = metaKey || ctrlKey

      if (['INPUT', 'TEXTAREA', 'BUTTON', 'A'].includes(tagName)) return

      if (code === 'Space') {
        if (videoRef.current.paused) {
          setFollowPlayhead(true)
          videoRef.current.play()
        } else {
          videoRef.current.pause()
        }
      }

      if (code === 'ArrowUp') {
        videoRef.current.pause()
        // eslint-disable-next-line no-param-reassign
        videoRef.current.currentTime = 0
      }

      if (code === 'ArrowDown') {
        videoRef.current.pause()
        // eslint-disable-next-line no-param-reassign
        videoRef.current.currentTime = videoRef.current.duration
      }

      if (code === 'ArrowLeft' && !shiftKey && !hasModifier) {
        videoRef.current.pause()
        // eslint-disable-next-line no-param-reassign
        videoRef.current.currentTime -= 0.1
      }

      if (code === 'ArrowRight' && !shiftKey && !hasModifier) {
        videoRef.current.pause()
        // eslint-disable-next-line no-param-reassign
        videoRef.current.currentTime += 0.1
      }

      if (code === 'ArrowLeft' && shiftKey && !hasModifier) {
        videoRef.current.pause()
        // eslint-disable-next-line no-param-reassign
        videoRef.current.currentTime -= 1
      }

      if (code === 'ArrowRight' && shiftKey && !hasModifier) {
        videoRef.current.pause()
        // eslint-disable-next-line no-param-reassign
        videoRef.current.currentTime += 1
      }

      if (code === 'ArrowLeft' && shiftKey && hasModifier) {
        gotoPreviousHit({ videoRef, timelines, settings })()
      }

      if (code === 'ArrowRight' && shiftKey && hasModifier) {
        gotoNextHit({ videoRef, timelines, settings })()
      }
    },
    [videoRef, timelines, settings],
  )

  useEffect(() => {
    document.addEventListener('keydown', keydownHandler)

    return () => document.removeEventListener('keydown', keydownHandler)
  }, [keydownHandler])

  return null
}

TimelineShortcuts.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      play: PropTypes.func,
      pause: PropTypes.func,
      currentTime: PropTypes.number,
      paused: PropTypes.bool,
      duration: PropTypes.number,
    }),
  }).isRequired,
  timelines: PropTypes.arrayOf(
    PropTypes.shape({
      timeline: PropTypes.string.isRequired,
      tracks: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
    }),
  ).isRequired,
  settings: PropTypes.shape({
    width: PropTypes.number.isRequired,
    filter: PropTypes.string.isRequired,
    timelines: PropTypes.shape({}).isRequired,
    zoom: PropTypes.number.isRequired,
  }).isRequired,
}

export default TimelineShortcuts
