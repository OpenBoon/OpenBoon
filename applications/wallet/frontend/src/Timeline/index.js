import PropTypes from 'prop-types'

import videoShape from '../Video/shape'

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
      minExpandedSize={MIN_HEIGHT}
      minCollapsedSize={BAR_HEIGHT}
      storageName="Timeline.height"
      openToThe="top"
    >
      {({ size, setSize, setStartingSize }) => (
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
              onClick={() => {
                reloadKey += 1
              }}
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

          {size >= MIN_HEIGHT && (
            <div css={{ height: TIMELINE_HEIGHT, overflow: 'auto' }}>
              <TimelineDetections />
            </div>
          )}
        </>
      )}
    </Resizeable>
  )
}

Timeline.propTypes = {
  videoRef: PropTypes.shape({
    current: videoShape,
  }).isRequired,
}

export default Timeline
