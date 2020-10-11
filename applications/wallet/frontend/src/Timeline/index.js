import { useMemo } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, spacing, constants } from '../Styles'

import DoubleChevronSvg from '../Icons/doubleChevron.svg'

import { useLocalStorage } from '../LocalStorage/helpers'

import Button, { VARIANTS } from '../Button'
import ResizeableWithMessage from '../Resizeable/WithMessage'

import { reducer, INITIAL_STATE, ACTIONS } from './reducer'
import { COLORS } from './helpers'

import TimelineControls from './Controls'
import TimelineCaptions from './Captions'
import TimelineModulesResizer from './ModulesResizer'
import TimelineFilterTracks from './FilterTracks'
import TimelineRuler from './Ruler'
import TimelinePlayhead from './Playhead'
import TimelineAggregate from './Aggregate'
import TimelineTimelines from './Timelines'
import TimelineMetadata from './Metadata'

const TIMELINE_HEIGHT = 200

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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [timelines])

  return (
    <ResizeableWithMessage
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
          <div css={{ flex: 1, padding: spacing.small, paddingLeft: 0 }}>
            <Button
              aria-label={`${isOpen ? 'Close' : 'Open'} Timeline`}
              variant={VARIANTS.ICON}
              style={{
                flexDirection: 'row',
                padding: spacing.small,
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
                }}
              />
              <div css={{ width: spacing.small }} />
              Timeline
            </Button>
          </div>

          <TimelineControls
            videoRef={videoRef}
            length={length}
            timelines={timelines}
            settings={settings}
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
          css={{
            display: 'flex',
            flexDirection: 'column',
            height: size,
          }}
        >
          <div
            css={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              height: '0%',
              position: 'relative',
              marginLeft: settings.width,
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

              <div css={{ flex: 1, overflow: 'overlay' }}>
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
    </ResizeableWithMessage>
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
