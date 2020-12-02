import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import { formatPaddedSeconds, gotoCurrentTime, GUIDE_WIDTH } from './helpers'

const WIDTH = GUIDE_WIDTH + spacing.mini * 2
const OFFSET = (WIDTH + constants.borderWidths.regular) / 2

const TimelineTracks = ({ videoRef, length, color, tracks, isOpen }) => {
  const duration = videoRef.current?.duration || length

  const aggregate = {}

  tracks.forEach(({ hits }) => {
    hits.forEach(({ start, stop, highlight }) => {
      if (!aggregate[start] || aggregate[start].highlight !== true) {
        aggregate[start] = { start, stop, highlight }
      }
    })
  })

  return (
    <div css={{ display: 'flex', flexDirection: 'column' }}>
      <div
        css={{
          position: 'relative',
          padding: spacing.base,
          borderBottom:
            color === colors.structure.white
              ? 'none'
              : constants.borders.regular.smoke,
          backgroundColor: colors.structure.soot,
        }}
      >
        &nbsp;
        {Object.values(aggregate).map(({ start, stop, highlight }) => (
          <button
            key={`${start}.${stop}`}
            type="button"
            onClick={gotoCurrentTime({ videoRef, start })}
            aria-label={`${formatPaddedSeconds({ seconds: start })}`}
            title={`${formatPaddedSeconds({
              seconds: start,
            })}-${formatPaddedSeconds({ seconds: stop })}`}
            css={{
              margin: 0,
              border: 0,
              zIndex: zIndex.layout.interactive + 1,
              position: 'absolute',
              top: spacing.base,
              bottom: spacing.base,
              left: `calc(${(start / duration) * 100}% - ${OFFSET}px)`,
              width: WIDTH,
              backgroundColor: colors.structure.soot,
              padding: spacing.mini,
              cursor: 'pointer',
            }}
          >
            <div
              css={{
                backgroundColor: color,
                width: '100%',
                height: '100%',
                position: 'relative',
              }}
            >
              {highlight && (
                <svg
                  width="8"
                  height="5"
                  css={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    zIndex: zIndex.layout.interactive + 2,
                  }}
                >
                  <polygon fill={color} points="0,0 8,0 6,2.5 8,5 0,5" />
                </svg>
              )}
            </div>
          </button>
        ))}
      </div>

      {isOpen &&
        tracks.map(({ track, hits }) => {
          return (
            <div
              key={track}
              css={{
                position: 'relative',
                padding: spacing.base,
                borderBottom: constants.borders.regular.smoke,
                backgroundColor: colors.structure.coal,
              }}
            >
              &nbsp;
              {hits.map(({ start, stop }) => (
                <button
                  key={`${track}.${start}.${stop}`}
                  type="button"
                  onClick={gotoCurrentTime({ videoRef, start })}
                  aria-label={`${formatPaddedSeconds({ seconds: start })}`}
                  title={`${formatPaddedSeconds({
                    seconds: start,
                  })}-${formatPaddedSeconds({ seconds: stop })}`}
                  css={{
                    margin: 0,
                    border: 0,
                    zIndex: zIndex.layout.interactive + 1,
                    position: 'absolute',
                    top: '25%',
                    bottom: '25%',
                    left: `calc(${(start / duration) * 100}% - ${OFFSET}px)`,
                    width: `calc(${
                      ((stop - start) / duration) * 100
                    }% + ${WIDTH}px)`,
                    backgroundColor: colors.structure.coal,
                    padding: spacing.mini,
                    cursor: 'pointer',
                  }}
                >
                  <div
                    css={{
                      backgroundColor: color,
                      width: '100%',
                      height: '100%',
                    }}
                  />
                </button>
              ))}
            </div>
          )
        })}
    </div>
  )
}

TimelineTracks.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      pause: PropTypes.func,
      currentTime: PropTypes.number,
      duration: PropTypes.number.isRequired,
    }),
  }).isRequired,
  length: PropTypes.number.isRequired,
  color: PropTypes.string.isRequired,
  tracks: PropTypes.arrayOf(
    PropTypes.shape({
      track: PropTypes.string.isRequired,
      hits: PropTypes.arrayOf(
        PropTypes.shape({
          start: PropTypes.number.isRequired,
          stop: PropTypes.number.isRequired,
        }).isRequired,
      ).isRequired,
    }).isRequired,
  ).isRequired,
  isOpen: PropTypes.bool.isRequired,
}

export default TimelineTracks
