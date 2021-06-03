import { useRef, useState, useCallback } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import PlaySvg from '../Icons/play.svg'
import PauseSvg from '../Icons/pause.svg'
import SeekSvg from '../Icons/seek.svg'
import FastForwardSvg from '../Icons/fast-forward.svg'

import Button, { VARIANTS } from '../Button'

import { formatPaddedSeconds, gotoNextHit, gotoPreviousHit } from './helpers'

const TimelineControls = ({
  videoRef,
  length,
  timelines,
  settings,
  setFollowPlayhead,
}) => {
  const frameRef = useRef()
  const isPausedRef = useRef(true)

  const [, setTick] = useState()

  const video = videoRef.current

  /* istanbul ignore next */
  const onMount = useCallback(
    (node) => {
      const animate = () => {
        if (node && video) {
          // eslint-disable-next-line no-param-reassign
          node.innerHTML = formatPaddedSeconds({
            seconds: video?.currentTime,
          })

          if (isPausedRef.current !== video?.paused) {
            isPausedRef.current = video?.paused

            setTick(performance.now())
          }
        }

        frameRef.current = requestAnimationFrame(animate)
      }

      frameRef.current = requestAnimationFrame(animate)

      if (frameRef.current && !node) {
        cancelAnimationFrame(frameRef.current)
        frameRef.current = null
      }

      if (!frameRef.current && node) {
        animate()
      }
    },
    [video],
  )

  return (
    <div
      css={{
        flex: 2,
        display: 'flex',
        alignItems: 'center',
        padding: spacing.small,
      }}
    >
      <div css={{ flex: 1, padding: spacing.small }} />

      <div css={{ display: 'flex' }}>
        <Button
          aria-label="Previous Detection"
          title="Previous Detection"
          variant={VARIANTS.ICON}
          style={{
            padding: spacing.small,
            ':hover, &.focus-visible:focus': {
              backgroundColor: colors.structure.mattGrey,
            },
          }}
          onClick={gotoPreviousHit({ videoRef, timelines, settings })}
        >
          <FastForwardSvg
            height={constants.icons.regular}
            css={{ transform: 'rotate(-180deg)' }}
          />
        </Button>

        <Button
          aria-label="Previous Second"
          title="Previous Second (Shift+Left)"
          variant={VARIANTS.ICON}
          style={{
            padding: spacing.small,
            ':hover, &.focus-visible:focus': {
              backgroundColor: colors.structure.mattGrey,
            },
          }}
          onClick={() => {
            video?.pause()
            video.currentTime = Math.trunc(video?.currentTime) - 1
          }}
        >
          <SeekSvg
            height={constants.icons.regular}
            css={{ transform: 'rotate(-180deg)' }}
          />
        </Button>

        <div css={{ width: spacing.small }} />

        <Button
          aria-label={video?.paused ? 'Play' : 'Pause'}
          title={video?.paused ? 'Play (Space)' : 'Pause (Space)'}
          variant={VARIANTS.ICON}
          style={{
            padding: spacing.small,
            ':hover, &.focus-visible:focus': {
              backgroundColor: colors.structure.mattGrey,
            },
          }}
          onClick={() => {
            if (video?.paused) {
              setFollowPlayhead(true)
              video?.play()
            } else {
              video?.pause()
            }
          }}
        >
          {video?.paused || !video ? (
            <PlaySvg height={constants.icons.regular} />
          ) : (
            <PauseSvg height={constants.icons.regular} />
          )}
        </Button>

        <div css={{ width: spacing.small }} />

        <Button
          aria-label="Next Second"
          title="Next Second (Shift+Right)"
          variant={VARIANTS.ICON}
          style={{
            padding: spacing.small,
            ':hover, &.focus-visible:focus': {
              backgroundColor: colors.structure.mattGrey,
            },
          }}
          onClick={() => {
            video?.pause()
            video.currentTime = Math.trunc(video?.currentTime) + 1
          }}
        >
          <SeekSvg height={constants.icons.regular} />
        </Button>

        <Button
          aria-label="Next Detection"
          title="Next Detection"
          variant={VARIANTS.ICON}
          style={{
            padding: spacing.small,
            ':hover, &.focus-visible:focus': {
              backgroundColor: colors.structure.mattGrey,
            },
          }}
          onClick={gotoNextHit({ videoRef, timelines, settings })}
        >
          <FastForwardSvg height={constants.icons.regular} />
        </Button>
      </div>

      <div
        css={{
          flex: 1,
          padding: spacing.small,
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <div
          ref={onMount}
          css={{
            backgroundColor: colors.structure.coal,
            padding: spacing.small,
            paddingLeft: spacing.base,
            paddingRight: spacing.base,
            color: colors.signal.sky.base,
          }}
        >
          {formatPaddedSeconds({ seconds: video?.currentTime })}
        </div>

        <div
          css={{
            padding: spacing.small,
            paddingLeft: spacing.base,
            paddingRight: spacing.base,
            whiteSpace: 'nowrap',
          }}
        >
          / {formatPaddedSeconds({ seconds: length })}
        </div>
      </div>
    </div>
  )
}

TimelineControls.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      play: PropTypes.func,
      pause: PropTypes.func,
      addEventListener: PropTypes.func,
      removeEventListener: PropTypes.func,
      currentTime: PropTypes.number,
      paused: PropTypes.bool,
    }),
  }).isRequired,
  length: PropTypes.number.isRequired,
  timelines: PropTypes.arrayOf(
    PropTypes.shape({
      timeline: PropTypes.string.isRequired,
      tracks: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
    }),
  ).isRequired,
  settings: PropTypes.shape({
    filter: PropTypes.string.isRequired,
    width: PropTypes.number.isRequired,
    timelines: PropTypes.shape({}).isRequired,
  }).isRequired,
  setFollowPlayhead: PropTypes.func.isRequired,
}

export default TimelineControls
