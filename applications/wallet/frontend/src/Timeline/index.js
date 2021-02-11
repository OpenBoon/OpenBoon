import { useMemo, useState } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { useScroller } from '../Scroll/helpers'

import { colors, spacing, constants } from '../Styles'

import DoubleChevronSvg from '../Icons/doubleChevron.svg'

import { useLocalStorage } from '../LocalStorage/helpers'
import { cleanup } from '../Filters/helpers'

import Button, { VARIANTS } from '../Button'
import CheckboxSwitch from '../Checkbox/Switch'
import Resizeable from '../Resizeable'

import { reducer, INITIAL_STATE, ACTIONS } from './reducer'
import { COLORS, GUIDE_WIDTH } from './helpers'

import TimelineControls from './Controls'
import TimelineCaptions from './Captions'
import TimelineModulesResizer from './ModulesResizer'
import TimelineFilterTracks from './FilterTracks'
import TimelineRuler from './Ruler'
import TimelinePlayhead from './Playhead'
import TimelineAggregate from './Aggregate'
import TimelineSearchHits from './SearchHits'
import TimelineTimelines from './Timelines'
import TimelineMetadata from './Metadata'
import TimelineShortcuts from './Shortcuts'
import TimelineResize from './Resize'

const TIMELINE_HEIGHT = 200
const SEPARATOR_WIDTH = 2

const Timeline = ({ videoRef, length }) => {
  const {
    query: { projectId, assetId, query },
  } = useRouter()

  const [settings, dispatch] = useLocalStorage({
    key: `TimelineTimelines.${assetId}`,
    reducer,
    initialState: INITIAL_STATE,
  })

  const cleanQuery = cleanup({ query })

  const { data: timelines } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/timelines/?query=${cleanQuery}`,
  )

  useMemo(() => {
    const value = timelines.reduce((acc, { timeline }, index) => {
      return {
        ...acc,
        [timeline]: {
          ...(settings.timelines[timeline] || {}),
          isOpen: settings.timelines[timeline]?.isOpen || false,
          isVisible: settings.timelines[timeline]?.isVisible || true,
          color: COLORS[index % COLORS.length],
        },
      }
    }, {})

    dispatch({ type: ACTIONS.UPDATE_TIMELINES, payload: { value } })

    if (cleanQuery === 'W10=' && settings.highlights) {
      dispatch({ type: ACTIONS.TOGGLE_HIGHLIGHTS })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const [followPlayhead, setFollowPlayhead] = useState(true)
  const handleOnWheel = () => setFollowPlayhead(false)

  const rulerRef = useScroller({
    namespace: 'Timeline',
    isWheelEmitter: true,
    isWheelListener: true,
    isScrollListener: true,
  })

  return (
    <Resizeable
      storageName={`Timeline.${assetId}`}
      minSize={TIMELINE_HEIGHT}
      openToThe="top"
      isInitiallyOpen
      header={({ isOpen, toggleOpen }) => (
        <div
          css={{
            paddingLeft: spacing.base,
            paddingRight: spacing.base,
            backgroundColor: colors.structure.lead,
            color: colors.structure.steel,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: constants.borders.regular.smoke,
          }}
        >
          <TimelineShortcuts
            videoRef={videoRef}
            timelines={timelines}
            settings={settings}
            setFollowPlayhead={setFollowPlayhead}
          />

          <div
            css={{
              flex: 1,
              padding: spacing.small,
              paddingLeft: 0,
              display: 'flex',
            }}
          >
            <Button
              aria-label={`${isOpen ? 'Close' : 'Open'} Timeline`}
              variant={VARIANTS.ICON}
              style={{
                flexDirection: 'row',
                alignItems: 'flex-end',
                padding: spacing.small,
                paddingRight: spacing.base,
                ':hover, &.focus-visible:focus': {
                  backgroundColor: colors.structure.mattGrey,
                  svg: {
                    path: {
                      fill: colors.structure.white,
                    },
                  },
                },
                textTransform: 'uppercase',
              }}
              onClick={toggleOpen}
            >
              <DoubleChevronSvg
                height={constants.icons.regular}
                color={colors.structure.steel}
                css={{
                  transform: `rotate(${isOpen ? 0 : -90}deg)`,
                  marginRight: spacing.small,
                }}
              />
              Timeline
            </Button>

            {cleanQuery !== 'W10=' && (
              <>
                <div
                  css={{
                    width: SEPARATOR_WIDTH,
                    backgroundColor: colors.structure.coal,
                    margin: spacing.small,
                  }}
                />

                <CheckboxSwitch
                  option={{
                    value: 'highlights',
                    label: (
                      <>
                        <svg width={12} height={14}>
                          <line
                            stroke="currentColor"
                            strokeWidth="4"
                            x1="0"
                            y1="0"
                            x2="0"
                            y2={14}
                          />
                          <polygon
                            fill="currentColor"
                            points="0,0 8,0 6,2.5 8,5 0,5"
                          />
                        </svg>
                        Search Only
                      </>
                    ),
                    initialValue: settings.highlights,
                    isDisabled: false,
                  }}
                  onClick={() => {
                    dispatch({ type: ACTIONS.TOGGLE_HIGHLIGHTS })
                  }}
                />
              </>
            )}
          </div>

          <TimelineControls
            videoRef={videoRef}
            length={length}
            timelines={timelines}
            settings={settings}
            setFollowPlayhead={setFollowPlayhead}
          />

          <div
            css={{
              display: 'flex',
              flex: 1,
              justifyContent: 'flex-end',
              padding: spacing.small,
              paddingRight: 0,
            }}
          >
            <TimelineCaptions videoRef={videoRef} initialTrackIndex={-1} />

            <TimelineMetadata videoRef={videoRef} assetId={assetId} />
          </div>
        </div>
      )}
    >
      {({ size }) => (
        <div
          aria-label="Timeline"
          css={{
            display: 'flex',
            flexDirection: 'column',
            height: size,
          }}
          onWheel={handleOnWheel}
        >
          <div
            css={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              height: '0%',
              position: 'relative',
              marginLeft: settings.width - GUIDE_WIDTH / 2,
              borderLeft: constants.borders.regular.smoke,
            }}
          >
            <TimelineModulesResizer settings={settings} dispatch={dispatch} />

            <TimelinePlayhead
              videoRef={videoRef}
              rulerRef={rulerRef}
              zoom={settings.zoom}
              followPlayhead={followPlayhead}
            />

            <div
              css={{
                display: 'flex',
                height: constants.timeline.rulerRowHeight,
              }}
            >
              <TimelineFilterTracks settings={settings} dispatch={dispatch} />

              <div ref={rulerRef} css={{ flex: 1, overflow: 'hidden' }}>
                <div css={{ width: `${settings.zoom}%` }}>
                  <TimelineRuler
                    videoRef={videoRef}
                    rulerRef={rulerRef}
                    length={videoRef.current?.duration || length}
                    settings={settings}
                  />
                </div>
              </div>
            </div>

            <TimelineAggregate
              videoRef={videoRef}
              length={length}
              timelineHeight={size}
              timelines={timelines}
              settings={settings}
              dispatch={dispatch}
            />

            {cleanQuery !== 'W10=' && (
              <TimelineSearchHits
                videoRef={videoRef}
                length={length}
                timelineHeight={size}
                timelines={timelines}
                settings={settings}
              />
            )}

            <TimelineTimelines
              videoRef={videoRef}
              length={length}
              timelines={timelines}
              settings={settings}
              dispatch={dispatch}
            />
            <TimelineResize
              dispatch={dispatch}
              zoom={settings.zoom}
              videoRef={videoRef}
              rulerRef={rulerRef}
            />
          </div>
        </div>
      )}
    </Resizeable>
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
      paused: PropTypes.bool,
      duration: PropTypes.number,
    }),
  }).isRequired,
  length: PropTypes.number.isRequired,
}

export default Timeline
