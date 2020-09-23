import PropTypes from 'prop-types'

import { colors, spacing, constants, typography } from '../Styles'

import TimebasedMetadataSvg from '../Icons/timebasedMetadata.svg'

import { useLocalStorageReducer } from '../LocalStorage/helpers'

import Button, { VARIANTS } from '../Button'
import ResizeableVertical from '../ResizeableVertical'

import { reducer } from './reducer'

import TimelineControls from './Controls'
import TimelineCaptions from './Captions'
import TimelineRuler from './Ruler'
import TimelinePlayhead from './Playhead'
import TimelineAggregate from './Aggregate'
import TimelineDetections from './Detections'

// TODO: fetch modules from backend
import detections from './__mocks__/detections'

const TIMELINE_HEIGHT = 300

const Timeline = ({ videoRef, length, assetId, cueIsOpen, setCueIsOpen }) => {
  const [settings, dispatch] = useLocalStorageReducer({
    key: `TimelineDetections.${assetId}`,
    reducer,
    initialState: {},
  })

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
              fontFamily: typography.family.condensed,
              textTransform: 'uppercase',
            }}
            onClick={toggleOpen}
          >
            Timeline
          </Button>

          <TimelineControls videoRef={videoRef} length={length} />

          <div css={{ display: 'flex', alignItems: 'center' }}>
            <TimelineCaptions videoRef={videoRef} initialTrackIndex={-1} />

            <div
              css={{
                width: spacing.mini,
                height: spacing.comfy,
                backgroundColor: colors.structure.coal,
                marginLeft: spacing.small,
                marginRight: spacing.base,
              }}
            />

            <Button
              aria-label={`${cueIsOpen ? 'Close' : 'Open'} Metadata`}
              variant={VARIANTS.ICON}
              style={{
                flexDirection: 'row',
                padding: spacing.small,
                ':hover, &.focus-visible:focus': {
                  backgroundColor: colors.structure.mattGrey,
                  color: cueIsOpen ? colors.key.one : colors.structure.white,
                  svg: {
                    path: {
                      fill: cueIsOpen ? colors.key.one : colors.structure.white,
                    },
                  },
                },
                fontFamily: typography.family.condensed,
                textTransform: 'uppercase',
                color: cueIsOpen ? colors.key.one : colors.structure.steel,
              }}
              onClick={() => setCueIsOpen(!cueIsOpen)}
            >
              <TimebasedMetadataSvg
                height={constants.icons.regular}
                color={cueIsOpen ? colors.key.one : colors.structure.steel}
              />
              <div css={{ width: spacing.base }} />
              Metadata
            </Button>
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
              marginLeft: constants.timeline.modulesWidth,
              borderLeft: constants.borders.regular.smoke,
            }}
          >
            <TimelinePlayhead videoRef={videoRef} />

            <TimelineRuler />

            <TimelineAggregate
              timelineHeight={size}
              detections={detections}
              settings={settings}
              dispatch={dispatch}
            />

            <TimelineDetections
              detections={detections}
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
    }),
  }).isRequired,
  length: PropTypes.number.isRequired,
  assetId: PropTypes.string.isRequired,
  cueIsOpen: PropTypes.bool.isRequired,
  setCueIsOpen: PropTypes.func.isRequired,
}

export default Timeline
