export const GUIDE_WIDTH = 2
export const MAJOR_TICK_HEIGHT = 16
export const MINOR_TICK_HEIGHT = 12
export const TICK_WIDTH = 2
export const HALF_SECOND = 0.5
export const MIN_TICK_SPACING = 32

export const formatPaddedSeconds = ({ seconds: s }) => {
  const seconds = Number.isFinite(s) ? s : 0

  const ISOString = new Date(seconds * 1000).toISOString()

  // has double digit hours
  if (seconds > 36000) return ISOString.substr(11, 8)

  // has single digit hours
  if (seconds > 3600) return `0${ISOString.substr(12, 7)}`

  // has double digit minutes
  if (seconds > 600) return `00:${ISOString.substr(14, 5)}`

  // has single digit minutes or less than 1 minute
  return `00:0${ISOString.substr(15, 4)}`
}

export const updatePlayheadPosition = ({ video, playhead }) => {
  if (!video || !playhead) return null

  return playhead.style.setProperty(
    'left',
    `calc(${(video.currentTime / video.duration) * 100}% - ${
      GUIDE_WIDTH / 2
    }px)`,
  )
}

export const filterDetections = ({ detections, settings }) => {
  return detections
    .map(({ name, predictions }) => {
      const filteredPredictions = predictions.filter((prediction) => {
        return prediction.label
          .toLowerCase()
          .includes(settings.filter.toLowerCase())
      })

      return { name, predictions: filteredPredictions }
    })
    .filter(({ predictions }) => predictions.length > 0)
}

export const getStep = ({ maxTicksCount, halfSeconds, majorStep }) => {
  const filteredTicks = halfSeconds.filter(
    (halfSecond) => halfSecond % (majorStep / 2) === 0,
  )

  if (filteredTicks.length > maxTicksCount) {
    const newStep = majorStep * 2
    return getStep({ maxTicksCount, halfSeconds, majorStep: newStep })
  }

  return majorStep
}

export const getRulerLayout = ({ length, width }) => {
  /**
   * calculate number of ticks that should show when width
   * is large enough to accommodate one tick for every half second
   */
  const halfSecondsCount = (length - (length % HALF_SECOND)) * 2

  const halfSeconds = Array.from({ length: halfSecondsCount }, (x, i) => i)

  /**
   * calculate the maximum number of ticks that would fit in the
   * given width while maintaining minimum spacing for legibility
   */
  const maxTicksCount = width / MIN_TICK_SPACING

  const majorStep = getStep({ maxTicksCount, halfSeconds, majorStep: 2 })

  return { halfSeconds, majorStep }
}

export const gotoCurrentTime = ({ videoRef, start }) => () => {
  videoRef.current.pause()
  // eslint-disable-next-line no-param-reassign
  videoRef.current.currentTime = start
}
