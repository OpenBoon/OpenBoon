import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Button, { VARIANTS } from '../Button'

import TimelineControls from './Controls'
import TimelineCaptions from './Captions'
import TimelinePlayhead from './Playhead'
import TimelineAggregate from './Aggregate'
import TimelineDetections from './Detections'

// TODO: fetch modules from backend
import detections from './__mocks__/detections'

// TODO: make resizeable height
const TIMELINE_HEIGHT = 300

const Timeline = ({ videoRef, length }) => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        height: TIMELINE_HEIGHT,
      }}
    >
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
        <div>
          <Button
            variant={VARIANTS.ICON}
            style={{
              padding: spacing.small,
              ':hover, &.focus-visible:focus': {
                backgroundColor: colors.structure.mattGrey,
              },
            }}
          >
            Timelime
          </Button>
        </div>

        <TimelineControls videoRef={videoRef} length={length} />

        <TimelineCaptions videoRef={videoRef} initialTrackIndex={-1} />
      </div>

      <div
        css={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          height: '0%',
          position: 'relative',
          marginLeft: constants.timeline.modulesWidth,
          borderLeft: constants.borders.regular.smoke,
        }}
      >
        <TimelinePlayhead videoRef={videoRef} />

        {/* TODO: add ruler and other stuff here */}
        <div css={{ height: constants.timeline.rulerRowHeight }} />

        <TimelineAggregate
          detections={detections}
          timelineHeight={TIMELINE_HEIGHT}
        />

        <TimelineDetections videoRef={videoRef} detections={detections} />
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
    }),
  }).isRequired,
  length: PropTypes.number.isRequired,
}

export default Timeline
