import { useCallback } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, spacing, constants } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import Button, { VARIANTS } from '../Button'
import ResizeableVertical from '../ResizeableVertical'

import { setScroll } from './helpers'
import { reducer, INITIAL_STATE } from './reducer'

import TimelineControls from './Controls'
import TimelineCaptions from './Captions'
import TimelineModulesResizer from './ModulesResizer'
import TimelineFilterTracks from './FilterTracks'
import TimelineRuler from './Ruler'
import TimelinePlayhead from './Playhead'
import TimelineAggregate from './Aggregate'
import TimelineTimelines from './Timelines'

const TIMELINE_HEIGHT = 200

let scrollLeftPos = 0
let scrollTopPos = 0

const Timeline = ({ videoRef, length }) => {
  const {
    query: { projectId, assetId },
  } = useRouter()

  const [settings, dispatch] = useLocalStorage({
    key: `TimelineTimelines.${assetId}`,
    reducer,
    initialState: INITIAL_STATE,
  })

  const { data: timelines } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/timelines/`,
  )

  /* istanbul ignore next */
  const onMount = useCallback((node) => {
    if (!node) return

    const scrollablesX = document.getElementsByClassName('scrollableX')
    const scrollablesY = document.getElementsByClassName('scrollableY')

    const handleOnWheel = (event) => {
      event.preventDefault()

      const { newScrollLeftPos, newScrollTopPos } = setScroll({
        event,
        scrollLeftPos,
        scrollTopPos,
        scrollablesX,
        scrollablesY,
      })

      scrollLeftPos = newScrollLeftPos
      scrollTopPos = newScrollTopPos
    }

    node.addEventListener('wheel', handleOnWheel, { passive: false })
  }, [])

  return (
    <ResizeableVertical
      storageName={`Timeline.${assetId}`}
      minHeight={TIMELINE_HEIGHT}
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
          <div css={{ flex: 1, padding: spacing.small, paddingLeft: 0 }}>
            <Button
              aria-label={`${isOpen ? 'Close' : 'Open'} Timeline`}
              variant={VARIANTS.ICON}
              style={{
                padding: spacing.small,
                ':hover, &.focus-visible:focus': {
                  backgroundColor: colors.structure.mattGrey,
                },
              }}
              onClick={toggleOpen}
            >
              Timeline
            </Button>
          </div>

          <TimelineControls videoRef={videoRef} length={length} />

          <TimelineCaptions videoRef={videoRef} initialTrackIndex={-1} />
        </div>
      )}
    >
      {({ size }) => (
        <div
          css={{
            display: 'flex',
            flexDirection: 'column',
            height: size,
          }}
        >
          <div
            ref={onMount}
            css={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              height: '0%',
              position: 'relative',
              marginLeft: settings.modulesWidth,
              borderLeft: constants.borders.regular.smoke,
            }}
          >
            <TimelineModulesResizer settings={settings} dispatch={dispatch} />

            <TimelinePlayhead videoRef={videoRef} zoom={settings.zoom} />

            <div
              css={{
                display: 'flex',
                height: constants.timeline.rulerRowHeight,
              }}
            >
              <TimelineFilterTracks settings={settings} dispatch={dispatch} />

              <div
                className="scrollableX"
                css={{ flex: 1, overflow: 'hidden' }}
              >
                <div css={{ width: `${settings.zoom}%` }}>
                  <TimelineRuler
                    length={videoRef.current?.duration || length}
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

            <TimelineTimelines
              videoRef={videoRef}
              length={length}
              timelines={timelines}
              settings={settings}
              dispatch={dispatch}
            />
          </div>
        </div>
      )}
    </ResizeableVertical>
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
