import PropTypes from 'prop-types'

import { useScroller } from '../Scroll/helpers'

import { constants } from '../Styles'

import { GUIDE_WIDTH } from './helpers'

import TimelineModulesResizer from './ModulesResizer'
import TimelineFilterTracks from './FilterTracks'
import TimelineRuler from './Ruler'
import TimelinePlayhead from './Playhead'
import TimelineAggregate from './Aggregate'
import TimelineSearchHits from './SearchHits'
import TimelineTimelines from './Timelines'
import TimelineScrollbar from './Scrollbar'

const Timeline = ({
  videoRef,
  length,
  settings,
  dispatch,
  cleanQuery,
  timelines,
  size,
  followPlayhead,
  stopFollowPlayhead,
}) => {
  const rulerRef = useScroller({
    namespace: 'Timeline',
    isWheelEmitter: true,
    isWheelListener: true,
    isScrollEmitter: true,
    isScrollListener: true,
  })

  return (
    <div
      aria-label="Timeline"
      css={{
        display: 'flex',
        flexDirection: 'column',
        height: size,
      }}
      onWheel={stopFollowPlayhead}
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
        <TimelineModulesResizer width={settings.width} dispatch={dispatch} />

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
          <TimelineFilterTracks
            width={settings.width}
            filter={settings.filter}
            dispatch={dispatch}
          />

          <div ref={rulerRef} css={{ flex: 1, overflow: 'hidden' }}>
            <div css={{ width: `${settings.zoom}%` }}>
              <TimelineRuler
                videoRef={videoRef}
                rulerRef={rulerRef}
                length={videoRef.current?.duration || length}
                width={settings.width}
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

        <TimelineScrollbar
          rulerRef={rulerRef}
          width={settings.width}
          initialZoom={settings.zoom}
          dispatch={dispatch}
          stopFollowPlayhead={stopFollowPlayhead}
        />
      </div>
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
      paused: PropTypes.bool,
      duration: PropTypes.number,
    }),
  }).isRequired,
  length: PropTypes.number.isRequired,
  settings: PropTypes.shape({
    filter: PropTypes.string.isRequired,
    width: PropTypes.number.isRequired,
    timelines: PropTypes.shape({}).isRequired,
    zoom: PropTypes.number.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
  cleanQuery: PropTypes.string.isRequired,
  timelines: PropTypes.arrayOf(
    PropTypes.shape({
      timeline: PropTypes.string.isRequired,
      tracks: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
    }),
  ).isRequired,
  size: PropTypes.number.isRequired,
  followPlayhead: PropTypes.bool.isRequired,
  stopFollowPlayhead: PropTypes.func.isRequired,
}

export default Timeline
