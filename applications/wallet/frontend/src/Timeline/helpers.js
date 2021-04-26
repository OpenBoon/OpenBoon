import { colors } from '../Styles'

export const GUIDE_WIDTH = 2
export const MAJOR_TICK_HEIGHT = 16
export const MINOR_TICK_HEIGHT = 12
export const TICK_WIDTH = 2
export const HALF_SECOND = 0.5
export const MIN_TICK_SPACING = 32
export const SCROLLBAR_CONTAINER_HEIGHT = 36
export const SCROLLBAR_RESIZE_HANDLE_SIZE = 20
export const SCROLLBAR_TRACK_MARGIN_WIDTH = 1

export const COLORS = [
  colors.graph.magenta,
  colors.signal.halloween.base,
  colors.signal.canary.base,
  colors.graph.seafoam,
  colors.graph.rust,
  colors.graph.coral,
  colors.graph.iris,
  colors.graph.marigold,
  colors.graph.magenta,
  colors.signal.grass.base,
]

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

export const updatePlayheadPosition = ({
  video,
  playhead,
  zoom,
  scrollLeft,
}) => {
  if (!video || !playhead) return null

  return playhead.style.setProperty(
    'left',
    `calc(${(video.currentTime / video.duration) * zoom}% - ${
      GUIDE_WIDTH / 2
    }px - ${scrollLeft}px)`,
  )
}

export const filterTimelines = ({ timelines, settings }) => {
  return timelines
    .map(({ timeline, tracks }) => {
      const filteredHitsTracks = settings.highlights
        ? tracks
            .map(({ track, hits }) => {
              return { track, hits: hits.filter(({ highlight }) => highlight) }
            })
            .filter(({ hits }) => hits.length > 0)
        : tracks

      const filteredPredictions = filteredHitsTracks.filter(({ track }) => {
        return track.toLowerCase().includes(settings.filter.toLowerCase())
      })

      return { timeline, tracks: filteredPredictions }
    })
    .filter(({ tracks }) => tracks.length > 0)
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

const getAdjacentHits = ({ currentTime, timelines, settings }) => {
  const sortedHits = filterTimelines({ timelines, settings })
    .filter(({ timeline }) => {
      return settings.timelines[timeline]?.isVisible !== false
    })
    .flatMap(({ tracks }) => {
      return tracks.flatMap(({ hits }) => {
        return hits.map(({ start }) => {
          return start
        })
      })
    })
    .sort((a, b) => a - b)

  const previousHits = sortedHits.filter(
    (hit) => Math.round(hit * 1000) < Math.round(currentTime * 1000),
  )
  const nextHits = sortedHits.filter(
    (hit) => Math.round(hit * 1000) > Math.round(currentTime * 1000),
  )

  return {
    previousHit: previousHits[previousHits.length - 1],
    nextHit: nextHits[0],
  }
}

export const gotoPreviousHit = ({ videoRef, timelines, settings }) => () => {
  videoRef.current.pause()

  const { currentTime } = videoRef.current

  const { previousHit } = getAdjacentHits({ currentTime, timelines, settings })

  // eslint-disable-next-line no-param-reassign
  videoRef.current.currentTime = previousHit || 0
}

export const gotoNextHit = ({ videoRef, timelines, settings }) => () => {
  videoRef.current.pause()

  const { currentTime, duration } = videoRef.current

  const { nextHit } = getAdjacentHits({ currentTime, timelines, settings })

  // eslint-disable-next-line no-param-reassign
  videoRef.current.currentTime = nextHit || duration
}

export const getNextScrollLeft = ({ videoRef, rulerRef, zoom, nextZoom }) => {
  const { currentTime = 0, duration = 0 } = videoRef.current || {}
  const { scrollWidth = 0, scrollLeft = 0, offsetWidth = 0 } =
    rulerRef.current || {}

  const playheadLeftOffset = (currentTime / duration) * scrollWidth - scrollLeft

  const isPlayheadOutOfView =
    playheadLeftOffset < 0 || playheadLeftOffset > offsetWidth

  const centeredPoint = (scrollLeft + offsetWidth / 2) / scrollWidth

  const nextScrollWidth = (scrollWidth / zoom) * nextZoom + GUIDE_WIDTH / 2

  const nextScrollLeft = isPlayheadOutOfView
    ? centeredPoint * nextScrollWidth - offsetWidth / 2
    : (currentTime / duration) * nextScrollWidth - playheadLeftOffset

  return nextScrollLeft
}

export const getScrollbarScrollableWidth = ({ scrollbarRef, zoom }) => {
  const { width: scrollbarWidth = 0 } =
    scrollbarRef.current?.getBoundingClientRect() || {}

  const scrollbarTrackWidth = scrollbarWidth * (zoom / 100)

  // the max number of pixels the scrollbar thumb can travel
  const scrollbarScrollableWidth = scrollbarTrackWidth - scrollbarWidth

  return scrollbarScrollableWidth
}
