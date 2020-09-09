import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Button, { VARIANTS } from '../Button'
import Resizeable from '../Resizeable'

import TimelineControls from './Controls'
import TimelineCaptions from './Captions'
import TimelinePlayhead from './Playhead'
import TimelineDetections from './Detections'

// TODO: make resizeable height
const TIMELINE_HEIGHT = 300

const BAR_HEIGHT = 43
const MIN_HEIGHT = 300

let reloadKey = 0

const Timeline = ({ videoRef }) => {
  return (
    <Resizeable
      key={reloadKey}
      minSize={BAR_HEIGHT}
      storageName="assetTimelineHeight"
      openToThe="top"
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
            aria-label="Open Timeline"
            variant={VARIANTS.ICON}
            style={{
              padding: spacing.small,
              ':hover, &.focus-visible:focus': {
                backgroundColor: colors.structure.mattGrey,
              },
            }}
            onClick={() => {
              reloadKey += 1

              if (height > BAR_HEIGHT) {
                setHeight({ value: BAR_HEIGHT })
              } else {
                setHeight({ value: MIN_HEIGHT })
              }
            }}
          >
            Timelime
          </Button>
        </div>

        <TimelineControls videoRef={videoRef} />

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

        <TimelineCaptions videoRef={videoRef} />
      </div>

      <div css={{ height: TIMELINE_HEIGHT, overflow: 'auto' }}>
        <TimelineDetections />
      </div>
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
      duration: PropTypes.number,
      paused: PropTypes.bool,
    }),
  }).isRequired,
}

export default Timeline
