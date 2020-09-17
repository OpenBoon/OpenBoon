import { useRef, useState, useEffect } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import PlaySvg from '../Icons/play.svg'
import PauseSvg from '../Icons/pause.svg'
import SeekSvg from '../Icons/seek.svg'

import Button, { VARIANTS } from '../Button'

import { formatPaddedSeconds } from './helpers'

const TimelineControls = ({ videoRef, length }) => {
  const currentTimeRef = useRef()
  const frameRef = useRef()

  const [, setTick] = useState()

  const video = videoRef.current
  const currentTime = currentTimeRef.current

  /* istanbul ignore next */
  useEffect(() => {
    const animate = () => {
      if (currentTime && video) {
        currentTime.innerHTML = formatPaddedSeconds({
          seconds: video?.currentTime,
        })

        setTick(performance.now())
      }

      frameRef.current = requestAnimationFrame(animate)
    }

    frameRef.current = requestAnimationFrame(animate)

    return () => cancelAnimationFrame(frameRef.current)
  }, [video, currentTime])

  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'center',
        padding: spacing.small,
      }}
    >
      <Button
        aria-label="Previous Second"
        variant={VARIANTS.ICON}
        style={{
          padding: spacing.small,
          ':hover, &.focus-visible:focus': {
            backgroundColor: colors.structure.mattGrey,
          },
        }}
        onClick={async () => {
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
        variant={VARIANTS.ICON}
        style={{
          padding: spacing.small,
          ':hover, &.focus-visible:focus': {
            backgroundColor: colors.structure.mattGrey,
          },
        }}
        onClick={async () => {
          if (video?.paused) {
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
        variant={VARIANTS.ICON}
        style={{
          padding: spacing.small,
          ':hover, &.focus-visible:focus': {
            backgroundColor: colors.structure.mattGrey,
          },
        }}
        onClick={async () => {
          video?.pause()
          video.currentTime = Math.trunc(video?.currentTime) + 1
        }}
      >
        <SeekSvg height={constants.icons.regular} />
      </Button>

      <div
        css={{
          padding: spacing.small,
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <div
          ref={currentTimeRef}
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
}

export default TimelineControls
