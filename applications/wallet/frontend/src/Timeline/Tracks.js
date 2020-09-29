import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import { formatPaddedSeconds, gotoCurrentTime, GUIDE_WIDTH } from './helpers'

const WIDTH = GUIDE_WIDTH + spacing.mini * 2
const OFFSET = (WIDTH + constants.borderWidths.regular) / 2

const TimelineTracks = ({ videoRef, length, moduleColor, tracks, isOpen }) => {
  const duration = videoRef.current?.duration || length

  return (
    <div css={{ display: 'flex', flexDirection: 'column' }}>
      <div
        css={{
          position: 'relative',
          padding: spacing.base,
          borderBottom: constants.borders.regular.smoke,
          backgroundColor: colors.structure.soot,
        }}
      >
        &nbsp;
        {tracks.map(({ track, hits }) => {
          return hits.map(({ start, stop }) => (
            <button
              key={`${track}.${start}`}
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
                  backgroundColor: moduleColor,
                  width: '100%',
                  height: '100%',
                }}
              />
            </button>
          ))
        })}
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
                  key={start}
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
                      backgroundColor: moduleColor,
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
  moduleColor: PropTypes.string.isRequired,
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
