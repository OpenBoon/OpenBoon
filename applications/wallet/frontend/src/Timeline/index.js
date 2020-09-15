import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import ResizeableVertical from '../ResizeableVertical'
import Button, { VARIANTS } from '../Button'

import TimelineControls from './Controls'
import TimelineCaptions from './Captions'
import TimelineContent from './Content'

const TIMELINE_HEIGHT = 300

const Timeline = ({ videoRef, assetId }) => {
  return (
    <ResizeableVertical
      storageName={`Timeline.${assetId}.height`}
      minExpandedSize={TIMELINE_HEIGHT}
      header={({ toggleOpen }) => (
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

          <TimelineControls videoRef={videoRef} />

          <TimelineCaptions videoRef={videoRef} initialTrackIndex={-1} />
        </div>
      )}
    >
      {({ size, originSize }) => (
        <TimelineContent
          isOpen={size >= TIMELINE_HEIGHT}
          size={size}
          originSize={originSize}
          videoRef={videoRef}
        />
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
