import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import { useScroller } from '../Scroll/helpers'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'

import {
  filterTimelines,
  formatPaddedSeconds,
  gotoCurrentTime,
  GUIDE_WIDTH,
} from './helpers'

import { ACTIONS } from './reducer'

const WIDTH = GUIDE_WIDTH + spacing.mini * 2
const OFFSET = (WIDTH + constants.borderWidths.regular) / 2

const TimelineAggregate = ({
  videoRef,
  length,
  timelineHeight,
  timelines,
  settings,
  dispatch,
}) => {
  const duration = videoRef.current?.duration || length

  const filteredTimelines = filterTimelines({ timelines, settings })

  const isAllVisible = Object.values(settings.timelines).every(
    ({ isVisible }) => isVisible === true,
  )

  const aggregateRef = useScroller({
    namespace: 'Timeline',
    isWheelEmitter: true,
    isWheelListener: true,
    isScrollEmitter: true,
    isScrollListener: true,
  })

  return (
    <div
      css={{
        display: 'flex',
        marginLeft: -settings.width,
        borderTop: constants.borders.regular.smoke,
        height: constants.timeline.rulerRowHeight,
      }}
    >
      <div
        css={{
          width: settings.width,
          zIndex: zIndex.timeline.menu,
          marginBottom: -spacing.hairline,
        }}
      >
        <Menu
          open="bottom-center"
          button={({ onBlur, onClick, isMenuOpen }) => (
            <MenuButton
              onBlur={onBlur}
              onClick={onClick}
              legend={`Timelines (${filteredTimelines.length})`}
              style={{
                '&,&:hover,&:visited': {
                  backgroundColor: isMenuOpen
                    ? colors.structure.steel
                    : colors.structure.smoke,
                },
                '&:hover': {
                  backgroundColor: colors.structure.steel,
                },
                '&[aria-disabled=true]': {
                  backgroundColor: colors.structure.smoke,
                },
                marginBottom: 0,
                borderRadius: 0,
                width: '100%',
                height: '100%',
              }}
            />
          )}
        >
          {() => (
            <div
              // eslint-disable-next-line
              tabIndex="1"
              css={{
                backgroundColor: colors.structure.iron,
                maxHeight:
                  timelineHeight - constants.timeline.rulerRowHeight * 2,
                overflowY: 'auto',
              }}
            >
              <div css={{ borderBottom: constants.borders.medium.steel }}>
                <Checkbox
                  key={isAllVisible}
                  variant={CHECKBOX_VARIANTS.MENU}
                  option={{
                    value: 'all',
                    label: 'All',
                    initialValue: isAllVisible,
                    isDisabled: false,
                  }}
                  onClick={() => {
                    dispatch({
                      type: ACTIONS.TOGGLE_VISIBLE_ALL,
                      payload: { timelines },
                    })
                  }}
                />
              </div>

              {timelines
                .sort((a, b) => (a.timeline > b.timeline ? 1 : -1))
                .map(({ timeline, tracks }) => (
                  <Checkbox
                    key={`${timeline}.${settings.timelines[timeline]?.isVisible}`}
                    variant={CHECKBOX_VARIANTS.MENU}
                    option={{
                      value: timeline,
                      label: timeline,
                      legend: `(${tracks.length})`,
                      initialValue:
                        settings.timelines[timeline]?.isVisible !== false,
                      isDisabled: false,
                    }}
                    onClick={() => {
                      dispatch({
                        type: ACTIONS.TOGGLE_VISIBLE,
                        payload: { timeline },
                      })
                    }}
                  />
                ))}
            </div>
          )}
        </Menu>
      </div>
      <div ref={aggregateRef} css={{ flex: 1, overflow: 'hidden' }}>
        <div
          css={{
            width: `${settings.zoom}%`,
            height: '100%',
            position: 'relative',
            padding: spacing.base,
            borderBottom: constants.borders.regular.coal,
            backgroundColor: colors.structure.coal,
          }}
        >
          &nbsp;
          {filteredTimelines
            .filter(({ timeline }) => {
              return settings.timelines[timeline]?.isVisible !== false
            })
            .map(({ tracks }) => {
              return tracks.map(({ track, hits }) => {
                return hits.map(({ start, stop }) => (
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
                      top: spacing.base,
                      bottom: spacing.base,
                      left: `calc(${(start / duration) * 100}% - ${OFFSET}px)`,
                      width: WIDTH,
                      backgroundColor: colors.structure.coal,
                      padding: spacing.mini,
                      cursor: 'pointer',
                      ':hover': {
                        div: {
                          backgroundColor: colors.structure.white,
                        },
                      },
                    }}
                  >
                    <div
                      css={{
                        backgroundColor: colors.structure.steel,
                        width: '100%',
                        height: '100%',
                      }}
                    />
                  </button>
                ))
              })
            })}
        </div>
      </div>
    </div>
  )
}

TimelineAggregate.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      pause: PropTypes.func,
      currentTime: PropTypes.number,
      duration: PropTypes.number.isRequired,
    }),
  }).isRequired,
  length: PropTypes.number.isRequired,
  timelineHeight: PropTypes.number.isRequired,
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
    zoom: PropTypes.number.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineAggregate
