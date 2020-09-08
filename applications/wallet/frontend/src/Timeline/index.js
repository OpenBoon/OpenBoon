import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import PlaySvg from '../Icons/play.svg'
import PauseSvg from '../Icons/pause.svg'
import SeekSvg from '../Icons/seek.svg'

import Button, { VARIANTS } from '../Button'

import { formatPaddedSeconds } from './helpers'

const Timeline = ({ videoRef }) => {
  const [, setTick] = useState()

  const video = videoRef.current

  useEffect(() => {
    /* istanbul ignore next */
    if (!video) return () => {}

    /* istanbul ignore next */
    const onDurationchange = (event) => {
      setTick(event.timeStamp)
    }

    video.addEventListener('durationchange', onDurationchange)

    return () => video.removeEventListener('durationchange', onDurationchange)
  }, [video])

  useEffect(() => {
    /* istanbul ignore next */
    if (!video) return () => {}

    /* istanbul ignore next */
    const onTimeupdate = (event) => {
      setTick(event.timeStamp)
    }

    video.addEventListener('timeupdate', onTimeupdate)

    return () => video.removeEventListener('timeupdate', onTimeupdate)
  }, [video])

  if (!video || !video.duration) return null

  return (
    <div>
      <div
        css={{
          paddingLeft: spacing.base,
          paddingRight: spacing.base,
          backgroundColor: colors.structure.lead,
          color: colors.structure.steel,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <div>
          <Button variant={VARIANTS.ICON}>Timelime</Button>
        </div>

        <div css={{ display: 'flex' }}>
          <Button
            aria-label="Previous Second"
            variant={VARIANTS.ICON}
            onClick={async () => {
              video.pause()
              video.currentTime = Math.trunc(video.currentTime) - 1
            }}
          >
            <SeekSvg
              height={constants.icons.regular}
              css={{ transform: 'rotate(-180deg)' }}
            />
          </Button>

          <Button
            aria-label={video.paused ? 'Play' : 'Pause'}
            variant={VARIANTS.ICON}
            onClick={async () => {
              if (video.paused) {
                video.play()
              } else {
                video.pause()
              }
            }}
          >
            {video.paused ? (
              <PlaySvg height={constants.icons.regular} />
            ) : (
              <PauseSvg height={constants.icons.regular} />
            )}
          </Button>

          <Button
            aria-label="Next Second"
            variant={VARIANTS.ICON}
            onClick={async () => {
              video.pause()
              video.currentTime = Math.trunc(video.currentTime) + 1
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
              css={{
                backgroundColor: colors.structure.coal,
                padding: spacing.small,
                paddingLeft: spacing.base,
                paddingRight: spacing.base,
                color: colors.signal.sky.base,
              }}
            >
              {formatPaddedSeconds({ seconds: video.currentTime })}
            </div>
            <div
              css={{
                padding: spacing.small,
                paddingLeft: spacing.base,
                paddingRight: spacing.base,
              }}
            >
              / {formatPaddedSeconds({ seconds: video.duration })}
            </div>
          </div>
        </div>

        <div>
          <Button variant={VARIANTS.ICON}>CC</Button>
        </div>
      </div>
      <div>{/* Insert Expanding Zone here */}</div>
    </div>
  )
}

Timeline.propTypes = {
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

export default Timeline
