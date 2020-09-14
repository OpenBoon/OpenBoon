import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import ResizeableMk2 from '../ResizeableMk2'
import Button, { VARIANTS } from '../Button'

import TimelineControls from './Controls'
import TimelineCaptions from './Captions'
import TimelinePlayhead from './Playhead'
import TimelineDetections from './Detections'

const BAR_HEIGHT = 43
const TIMELINE_HEIGHT = 300

const Timeline = ({ videoRef }) => {
  return (
    <ResizeableMk2
      storageName="Timeline.height"
      minExpandedSize={TIMELINE_HEIGHT}
      collapsedSize={BAR_HEIGHT}
      openToThe="top"
    >
      {({ size, toggleOpen, renderCopy }) => {
        const isOpen = size >= TIMELINE_HEIGHT

        return (
          <>
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
                onClick={toggleOpen}
              >
                Timelime
              </Button>
            </div>
            <TimelineControls />
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

            <div css={{ height: size - BAR_HEIGHT, overflow: 'auto' }}>
              {isOpen && <TimelineDetections />}

              {!isOpen && renderCopy()}
            </div>
          </>
        )
      }}
    </ResizeableMk2>
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
