import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import ResizeableVertical from '../ResizeableVertical'
import Button, { VARIANTS } from '../Button'

import TimelineControls from './Controls'
import TimelineCaptions from './Captions'
import TimelinePlayhead from './Playhead'
import TimelineDetections from './Detections'
import TimelineDropMessage from './DropMessage'

const TIMELINE_HEIGHT = 300

const Timeline = ({ videoRef, assetId }) => {
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
            Timelime
          </Button>

          <TimelineControls videoRef={videoRef} />

          <TimelineCaptions videoRef={videoRef} initialTrackIndex={-1} />
        </div>
      )}
    >
      {({ size, isOpen, originSize }) => (
        <div css={{ height: size, overflow: 'auto' }}>
          {isOpen ? (
            <div
              css={{
                display: 'flex',
                flexDirection: 'column',
                height: size,
                overflow: 'hidden',
              }}
            >
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

                <TimelineDetections videoRef={videoRef} />
              </div>
            </div>
          ) : (
            /* istanbul ignore next */ <TimelineDropMessage
              size={size}
              originSize={originSize}
            />
          )}
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
      duration: PropTypes.number,
      paused: PropTypes.bool,
    }),
  }).isRequired,
  assetId: PropTypes.string.isRequired,
}

export default Timeline
